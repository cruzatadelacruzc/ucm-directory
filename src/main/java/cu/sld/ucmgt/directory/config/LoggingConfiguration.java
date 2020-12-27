package cu.sld.ucmgt.directory.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.boolex.OnMarkerEvaluator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.filter.EvaluatorFilter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.composite.ContextJsonProvider;
import net.logstash.logback.composite.GlobalCustomFieldsJsonProvider;
import net.logstash.logback.composite.loggingevent.*;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.stacktrace.ShortenedThrowableConverter;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;


import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/*
 * Configures the console and Logstash log appenders from the app properties
 */
@Slf4j
@Configuration
@RefreshScope
public class LoggingConfiguration {

    public LoggingConfiguration(@Value("${spring.application.name}") String appName,
                                @Value("${server.port}") String serverPort,
                                AppProperties properties,
                                ObjectProvider<BuildProperties> buildProperties,
                                ObjectMapper mapper) throws JsonProcessingException {

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Map<String, String> map = new HashMap<>();
        map.put("app_name", appName);
        map.put("app_port", serverPort);
        buildProperties.ifAvailable(entries -> map.put("version", entries.getVersion()));
        String customFields = mapper.writeValueAsString(map);
        AppProperties.Logging logging = properties.getLogging();
        AppProperties.Logging.Logstash logstash = logging.getLogstash();

        if (logging.isUseJsonFormat()) {
            addJsonConsoleAppender(context, customFields);
        }

        if (logstash.isEnabled()) {
            addLogstashTcpSocketAppender(context, customFields, logstash);
        }

        if (logstash.isEnabled() || logging.isUseJsonFormat()) {
            addContextListener(context, customFields, logging);
        }

        if (properties.getMetrics().getLogs().isEnabled()) {
            setMetricsMarkerLogbackFilter(context, logging.isUseJsonFormat());
        }
    }

    public static void addJsonConsoleAppender(LoggerContext context, String customFields) {
        log.info("Initializing Console loggingProperties");
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender();
        consoleAppender.setContext(context);
        consoleAppender.setEncoder(compositeJsonEncoder(context, customFields));
        consoleAppender.setName("CONSOLE");
        consoleAppender.start();
        context.getLogger("ROOT").detachAppender("CONSOLE");
        context.getLogger("ROOT").addAppender(consoleAppender);
    }

    public static void addLogstashTcpSocketAppender(LoggerContext context, String customFields, AppProperties.Logging.Logstash logstashProperties) {
        log.info("Initializing Logstash loggingProperties");
        LogstashTcpSocketAppender logstashAppender = new LogstashTcpSocketAppender();
        logstashAppender.addDestinations(new InetSocketAddress[]{new InetSocketAddress(logstashProperties.getHost(), logstashProperties.getPort())});
        logstashAppender.setContext(context);
        logstashAppender.setEncoder(logstashEncoder(customFields));
        logstashAppender.setName("ASYNC_LOGSTASH");
        logstashAppender.setQueueSize(logstashProperties.getQueueSize());
        logstashAppender.start();
        context.getLogger("ROOT").addAppender(logstashAppender);
    }

    public static void addContextListener(LoggerContext context, String customFields, AppProperties.Logging properties) {
        LogbackLoggerContextListener loggerContextListener = new LogbackLoggerContextListener(properties, customFields);
        loggerContextListener.setContext(context);
        context.addListener(loggerContextListener);
    }

    public static void setMetricsMarkerLogbackFilter(LoggerContext context, boolean useJsonFormat) {
        log.info("Filtering metrics logs from all appenders except the {} appender", "LOGSTASH");
        OnMarkerEvaluator onMarkerMetricsEvaluator = new OnMarkerEvaluator();
        onMarkerMetricsEvaluator.setContext(context);
        onMarkerMetricsEvaluator.addMarker("metrics");
        onMarkerMetricsEvaluator.start();
        EvaluatorFilter<ILoggingEvent> metricsFilter = new EvaluatorFilter();
        metricsFilter.setContext(context);
        metricsFilter.setEvaluator(onMarkerMetricsEvaluator);
        metricsFilter.setOnMatch(FilterReply.DENY);
        metricsFilter.start();
        context.getLoggerList().forEach((logger) -> {
            logger.iteratorForAppenders().forEachRemaining((appender) -> {
                if (!appender.getName().equals("ASYNC_LOGSTASH") && (!appender.getName().equals("CONSOLE") || !useJsonFormat)) {
                    log.debug("Filter metrics logs from the {} appender", appender.getName());
                    appender.setContext(context);
                    appender.addFilter(metricsFilter);
                    appender.start();
                }

            });
        });
    }

    private static LoggingEventCompositeJsonEncoder compositeJsonEncoder(LoggerContext context, String customFields) {
        LoggingEventCompositeJsonEncoder compositeJsonEncoder = new LoggingEventCompositeJsonEncoder();
        compositeJsonEncoder.setContext(context);
        compositeJsonEncoder.setProviders(jsonProviders(context, customFields));
        compositeJsonEncoder.start();
        return compositeJsonEncoder;
    }

    private static LogstashEncoder logstashEncoder(String customFields) {
        LogstashEncoder logstashEncoder = new LogstashEncoder();
        logstashEncoder.setThrowableConverter(throwableConverter());
        logstashEncoder.setCustomFields(customFields);
        return logstashEncoder;
    }

    private static LoggingEventJsonProviders jsonProviders(LoggerContext context, String customFields) {
        LoggingEventJsonProviders jsonProviders = new LoggingEventJsonProviders();
        jsonProviders.addArguments(new ArgumentsJsonProvider());
        jsonProviders.addContext(new ContextJsonProvider());
        jsonProviders.addGlobalCustomFields(customFieldsJsonProvider(customFields));
        jsonProviders.addLogLevel(new LogLevelJsonProvider());
        jsonProviders.addLoggerName(loggerNameJsonProvider());
        jsonProviders.addMdc(new MdcJsonProvider());
        jsonProviders.addMessage(new MessageJsonProvider());
        jsonProviders.addPattern(new LoggingEventPatternJsonProvider());
        jsonProviders.addStackTrace(stackTraceJsonProvider());
        jsonProviders.addThreadName(new ThreadNameJsonProvider());
        jsonProviders.addTimestamp(timestampJsonProvider());
        jsonProviders.setContext(context);
        return jsonProviders;
    }

    private static GlobalCustomFieldsJsonProvider<ILoggingEvent> customFieldsJsonProvider(String customFields) {
        GlobalCustomFieldsJsonProvider<ILoggingEvent> customFieldsJsonProvider = new GlobalCustomFieldsJsonProvider();
        customFieldsJsonProvider.setCustomFields(customFields);
        return customFieldsJsonProvider;
    }

    private static LoggerNameJsonProvider loggerNameJsonProvider() {
        LoggerNameJsonProvider loggerNameJsonProvider = new LoggerNameJsonProvider();
        loggerNameJsonProvider.setShortenedLoggerNameLength(20);
        return loggerNameJsonProvider;
    }

    private static StackTraceJsonProvider stackTraceJsonProvider() {
        StackTraceJsonProvider stackTraceJsonProvider = new StackTraceJsonProvider();
        stackTraceJsonProvider.setThrowableConverter(throwableConverter());
        return stackTraceJsonProvider;
    }

    private static ShortenedThrowableConverter throwableConverter() {
        ShortenedThrowableConverter throwableConverter = new ShortenedThrowableConverter();
        throwableConverter.setRootCauseFirst(true);
        return throwableConverter;
    }

    private static LoggingEventFormattedTimestampJsonProvider timestampJsonProvider() {
        LoggingEventFormattedTimestampJsonProvider timestampJsonProvider = new LoggingEventFormattedTimestampJsonProvider();
        timestampJsonProvider.setTimeZone("UTC");
        timestampJsonProvider.setFieldName("timestamp");
        return timestampJsonProvider;
    }

    private static class LogbackLoggerContextListener extends ContextAwareBase implements LoggerContextListener {
        private final AppProperties.Logging loggingProperties;
        private final String customFields;

        private LogbackLoggerContextListener(AppProperties.Logging loggingProperties, String customFields) {
            this.loggingProperties = loggingProperties;
            this.customFields = customFields;
        }

        public boolean isResetResistant() {
            return true;
        }

        public void onStart(LoggerContext context) {
            if (this.loggingProperties.isUseJsonFormat()) {
               addJsonConsoleAppender(context, this.customFields);
            }

            if (this.loggingProperties.getLogstash().isEnabled()) {
                addLogstashTcpSocketAppender(context, this.customFields, this.loggingProperties.getLogstash());
            }

        }

        public void onReset(LoggerContext context) {
            if (this.loggingProperties.isUseJsonFormat()) {
                addJsonConsoleAppender(context, this.customFields);
            }

            if (this.loggingProperties.getLogstash().isEnabled()) {
                addLogstashTcpSocketAppender(context, this.customFields, this.loggingProperties.getLogstash());
            }

        }

        public void onStop(LoggerContext context) {
        }

        public void onLevelChange(ch.qos.logback.classic.Logger logger, Level level) {
        }
    }
}
