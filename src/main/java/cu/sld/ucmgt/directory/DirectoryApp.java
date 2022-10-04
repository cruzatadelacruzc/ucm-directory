package cu.sld.ucmgt.directory;

import cu.sld.ucmgt.directory.config.AppProperties;
import cu.sld.ucmgt.directory.config.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class})
@EnableJpaRepositories("cu.sld.ucmgt.directory.repository")
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
@EnableElasticsearchRepositories("cu.sld.ucmgt.directory.repository.search")
public class DirectoryApp {

	private final Environment env;

	public DirectoryApp(Environment env) {
		this.env = env;
	}

	/**
	 * Initializes directory.
	 * <p>
	 * Spring profiles can be configured with a program argument --spring.profiles.active=your-active-profile
	 * <p>
	 */
	@PostConstruct
	public void initApplication() {
		Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
		if (activeProfiles.contains(Constants.PROFILE_DEV) && activeProfiles.contains(Constants.PROFILE_PROD)) {
			log.error("You have misconfigured your application! It should not run " +
					"with both the 'dev' and 'prod' profiles at the same time.");
		}
	}

	/**
	 * Main method, used to run the application.
	 *
	 * @param args the command line arguments.
	 */
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(DirectoryApp.class);
		addDefaultProfile(app);
		Environment env = app.run(args).getEnvironment();
		logApplicationStartup(env);
	}


	/**
	 * Set a default to use when no profile is configured.
	 *
	 * @param app the Spring application.
	 */
	public static void addDefaultProfile(SpringApplication app) {
		Map<String, Object> defProperties = new HashMap<>();
		/*
		 * The default profile to use when no other profiles are defined
		 * This cannot be set in the application.yml file.
		 * See https://github.com/spring-projects/spring-boot/issues/1219
		 */
		defProperties.put("spring.profiles.default", Constants.PROFILE_DEV);
		app.setDefaultProperties(defProperties);
	}

	private static void logApplicationStartup(Environment env) {
		String protocol = "http";
		if (env.getProperty("server.ssl.key-store") != null) {
			protocol = "https";
		}
		String serverPort = env.getProperty("server.port");
		String contextPath = env.getProperty("server.servlet.context-path");
		if (StringUtils.isBlank(contextPath)) {
			contextPath = "/";
		}
		String hostAddress = "localhost";
		try {
			hostAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			log.warn("The host name could not be determined, using `localhost` as fallback");
		}
		log.info("\n----------------------------------------------------------\n\t" +
						"Application '{}' is running! Access URLs:\n\t" +
						"Local: \t\t{}://localhost:{}{}\n\t" +
						"External: \t{}://{}:{}{}\n\t" +
						"Profile(s): \t{}\n----------------------------------------------------------",
				env.getProperty("spring.application.name"),
				protocol,
				serverPort,
				contextPath,
				protocol,
				hostAddress,
				serverPort,
				contextPath,
				env.getActiveProfiles());

		String configServerStatus = env.getProperty("configserver.status");
		if (configServerStatus == null) {
			configServerStatus = "Not found or not setup for this application";
		}
		log.info("\n----------------------------------------------------------\n\t" +
				"Config Server: \t{}\n----------------------------------------------------------", configServerStatus);
	}

}
