package cu.sld.ucmgt.directory.config;


import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Properties specific to Directory.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 */
@Getter
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class AppProperties {

    private final Cache cache = new Cache();
    private final Metrics metrics = new Metrics();
    private final Swagger swagger = new Swagger();
    private final Logging logging = new Logging();
    private final Security security = new Security();
    private final ClientApp clientApp = new ClientApp();
    private final AuditEvents auditEvents = new AuditEvents();
    private final CorsConfiguration cors = new CorsConfiguration();
    private final RegistryConfig registryConfig = new RegistryConfig();

    @Getter
    public static class Cache {
        private int timeToLiveSeconds = 3600;
        private int backupCount = 1;
        private final ManagementCenter managementCenter = new ManagementCenter();

        public Cache setTimeToLiveSeconds(int timeToLiveSeconds) {
            this.timeToLiveSeconds = timeToLiveSeconds;
            return this;
        }

        public Cache setBackupCount(int backupCount) {
            this.backupCount = backupCount;
            return this;
        }

        @Getter
        public static class ManagementCenter {
            private boolean enabled = false;
            private int updateInterval = 3;
            private String url = "";

            public ManagementCenter setEnabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public ManagementCenter setUpdateInterval(int updateInterval) {
                this.updateInterval = updateInterval;
                return this;
            }

            public ManagementCenter setUrl(String url) {
                this.url = url;
                return this;
            }
        }
    }

    @Getter
    public static class AuditEvents {
        private int retentionPeriod = 30;

        public AuditEvents setRetentionPeriod(int retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
            return this;
        }
    }
    @Getter
    public static class Metrics {
        private final Logs logs = new Logs();

        @Getter
        public static class Logs {
            private boolean enabled = false;
            private long reportFrequency = 60L;

            public Logs setEnabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public Logs setReportFrequency(long reportFrequency) {
                this.reportFrequency = reportFrequency;
                return this;
            }
        }
    }

    @Getter
    public static class Swagger {
        private String title = "Application API";
        private String description = "API documentation";
        private String version = "0.0.1";
        private String termsOfServiceUrl;
        private String contactName;
        private String contactUrl;
        private String contactEmail;
        private String license;
        private String licenseUrl;
        private String defaultIncludePattern = "/api/.*";
        private String host;
        private String[] protocols = new String[0];
        ;
        private boolean useDefaultResponseMessages = true;

        public Swagger setTitle(String title) {
            this.title = title;
            return this;
        }

        public Swagger setDescription(String description) {
            this.description = description;
            return this;
        }

        public Swagger setVersion(String version) {
            this.version = version;
            return this;
        }

        public Swagger setTermsOfServiceUrl(String termsOfServiceUrl) {
            this.termsOfServiceUrl = termsOfServiceUrl;
            return this;
        }

        public Swagger setContactName(String contactName) {
            this.contactName = contactName;
            return this;
        }

        public Swagger setContactUrl(String contactUrl) {
            this.contactUrl = contactUrl;
            return this;
        }

        public Swagger setContactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
            return this;
        }

        public Swagger setLicense(String license) {
            this.license = license;
            return this;
        }

        public Swagger setLicenseUrl(String licenseUrl) {
            this.licenseUrl = licenseUrl;
            return this;
        }

        public Swagger setDefaultIncludePattern(String defaultIncludePattern) {
            this.defaultIncludePattern = defaultIncludePattern;
            return this;
        }

        public Swagger setHost(String host) {
            this.host = host;
            return this;
        }

        public Swagger setProtocols(String[] protocols) {
            this.protocols = protocols;
            return this;
        }

        public Swagger setUseDefaultResponseMessages(boolean useDefaultResponseMessages) {
            this.useDefaultResponseMessages = useDefaultResponseMessages;
            return this;
        }
    }

    @Getter
    public static class Logging {
        private boolean useJsonFormat = false;
        private final Logstash logstash = new Logstash();

        public void setUseJsonFormat(boolean useJsonFormat) {
            this.useJsonFormat = useJsonFormat;
        }

        @Getter
        public static class Logstash {
            private boolean enabled = false;
            private String host = "localhost";
            private int port = 5000;
            private int queueSize = 512;

            public Logstash setEnabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public Logstash setHost(String host) {
                this.host = host;
                return this;
            }

            public Logstash setPort(int port) {
                this.port = port;
                return this;
            }

            public Logstash setQueueSize(int queueSize) {
                this.queueSize = queueSize;
                return this;
            }
        }
    }

    @Getter
    public static class Security {

        private final OAuth2 oauth2 = new OAuth2();

        public static class OAuth2 {
            private List<String> audience = new ArrayList();

            public List<String> getAudience() {
                return Collections.unmodifiableList(this.audience);
            }

            public void setAudience(@NotNull List<String> audience) {
                this.audience.addAll(audience);
            }
        }
    }

    @Getter
    public static class ClientApp {
        private String name = "directoryApp";

        public ClientApp setName(String name) {
            this.name = name;
            return this;
        }
    }

    @Getter
    public static class RegistryConfig {
        private String password;

        public RegistryConfig setPassword(String password) {
            this.password = password;
            return this;
        }
    }


}
