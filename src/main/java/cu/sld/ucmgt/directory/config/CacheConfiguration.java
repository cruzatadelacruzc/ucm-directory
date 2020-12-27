package cu.sld.ucmgt.directory.config;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfiguration {

    private final Environment env;
    private Registration registration;
    private GitProperties gitProperties;
    private BuildProperties buildProperties;
    private final DiscoveryClient discoveryClient;
    private final ServerProperties serverProperties;

    @Autowired(required = false)
    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    @PreDestroy
    public void destroy() {
        log.info("Closing Cache Manager");
        Hazelcast.shutdownAll();
    }

    @Bean
    public CacheManager cacheManager(HazelcastInstance hazelcastInstance) {
        log.debug("Starting HazelcastCacheManager");
        return new com.hazelcast.spring.cache.HazelcastCacheManager(hazelcastInstance);
    }

    @Bean
    public HazelcastInstance hazelcastInstance(AppProperties properties) {
        log.debug("Configuring Hazelcast");
        HazelcastInstance hazelCastInstance = Hazelcast.getHazelcastInstanceByName("directory");
        if (hazelCastInstance != null) {
            log.debug("Hazelcast already initialized");
            return hazelCastInstance;
        }
        Config config = new Config();
        config.setInstanceName("directory");
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        if (this.registration == null) {
            log.warn("No discovery service is set up, Hazelcast cannot create a cluster.");
        } else {
            // The serviceId is by default the application's name,
            // see the "spring.application.name" standard Spring property
            String serviceId = registration.getServiceId();
            log.debug("Configuring Hazelcast clustering for instanceId: {}", serviceId);
            // In development, everything goes through 127.0.0.1, with a different port
            if (env.acceptsProfiles(Profiles.of(Constants.PROFILE_DEV))) {
                log.debug("Application is running with the \"dev\" profile, Hazelcast " +
                        "cluster will only work with localhost instances");

                System.setProperty("hazelcast.local.localAddress", "127.0.0.1");
                config.getNetworkConfig().setPort(serverProperties.getPort() + 5701);
                config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
                for (ServiceInstance instance : discoveryClient.getInstances(serviceId)) {
                    String clusterMember = "127.0.0.1:" + (instance.getPort() + 5701);
                    log.debug("Adding Hazelcast (dev) cluster member {}", clusterMember);
                    config.getNetworkConfig().getJoin().getTcpIpConfig().addMember(clusterMember);
                }
        } else { // Production configuration, one host per instance all using port 5701
                config.getNetworkConfig().setPort(5701);
                config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
                for (ServiceInstance instance : discoveryClient.getInstances(serviceId)) {
                    String clusterMember = instance.getHost() + ":5701";
                    log.debug("Adding Hazelcast (prod) cluster member {}", clusterMember);
                    config.getNetworkConfig().getJoin().getTcpIpConfig().addMember(clusterMember);
                }
            }
    }
        config.getMapConfigs().put("default", initializeDefaultMapConfig(properties));
        // Full reference is available at: https://docs.hazelcast.org/docs/management-center/3.9/manual/html/Deploying_and_Starting.html
        config.setManagementCenterConfig(initializeDefaultManagementCenterConfig(properties));
        config.getMapConfigs().put("cu.sld.ucmgt.directory.domain.*", initializeDomainMapConfig(properties));
        return Hazelcast.newHazelcastInstance(config);
    }

    private ManagementCenterConfig initializeDefaultManagementCenterConfig(AppProperties properties) {
        ManagementCenterConfig managementCenterConfig = new ManagementCenterConfig();
        managementCenterConfig.setEnabled(properties.getCache().getManagementCenter().isEnabled());
        managementCenterConfig.setUrl(properties.getCache().getManagementCenter().getUrl());
        managementCenterConfig.setUpdateInterval(properties.getCache().getManagementCenter().getUpdateInterval());
        return managementCenterConfig;
    }

    private MapConfig initializeDefaultMapConfig(AppProperties properties) {
        MapConfig mapConfig = new MapConfig();

        /*
        Number of backups. If 1 is set as the backup-count for example,
        then all entries of the map will be copied to another JVM for
        fail-safety. Valid numbers are 0 (no backup), 1, 2, 3.
        */
        mapConfig.setBackupCount(properties.getCache().getBackupCount());

        /*
        Valid values are:
        NONE (no eviction),
        LRU (Least Recently Used),
        LFU (Least Frequently Used).
        NONE is the default.
        */
        mapConfig.setEvictionPolicy(EvictionPolicy.LRU);

        /*
        Maximum size of the map. When max size is reached,
        map is evicted based on the policy defined.
        Any integer between 0 and Integer.MAX_VALUE. 0 means
        Integer.MAX_VALUE. Default is 0.
        */
        mapConfig.setMaxSizeConfig(new MaxSizeConfig(0, MaxSizeConfig.MaxSizePolicy.USED_HEAP_SIZE));

        return mapConfig;
    }

    private MapConfig initializeDomainMapConfig(AppProperties properties) {
        MapConfig mapConfig = new MapConfig();
        mapConfig.setTimeToLiveSeconds(properties.getCache().getTimeToLiveSeconds());
        return mapConfig;
    }

    @Autowired(required = false)
    public void setGitProperties(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
    }

    @Autowired(required = false)
    public void setBuildProperties(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    /**
     *  Explanation is available at https://www.baeldung.com/spring-cache-custom-keygenerator
     * @return {@link KeyGenerator} instance
     */
    @Bean
    public KeyGenerator keyGenerator() {
        return new PrefixedKeyGenerator(this.gitProperties, this.buildProperties);
    }

    @Getter
    private static class PrefixedKeyGenerator implements KeyGenerator {

        private final String prefix;

        public PrefixedKeyGenerator(GitProperties gitProperties, BuildProperties buildProperties) {
            this.prefix = this.generatePrefix(gitProperties, buildProperties);
        }

        private String generatePrefix(GitProperties gitProperties, BuildProperties buildProperties) {
            String shortCommitId = null;
            if (Objects.nonNull(gitProperties)) {
                shortCommitId = gitProperties.getShortCommitId();
            }

            Instant time = null;
            String version = null;
            if (Objects.nonNull(buildProperties)) {
                time = buildProperties.getTime();
                version = buildProperties.getVersion();
            }

            Object p = ObjectUtils.firstNonNull(new Serializable[]{shortCommitId, time, version, RandomStringUtils.randomAlphanumeric(12)});
            return p instanceof Instant ? DateTimeFormatter.ISO_INSTANT.format((Instant)p) : p.toString();
        }

        @Override
        public Object generate(Object o, Method method, Object... objects) {
            return new PrefixedSimpleKey(this.prefix, method.getName(), objects);
        }

        private static class PrefixedSimpleKey {
            private final String prefix;
            private final Object[] params;
            private final String methodName;
            private int hashCode;

            public PrefixedSimpleKey(String prefix, String methodName, Object... elements) {
                Assert.notNull(prefix, "Prefix must not be null");
                Assert.notNull(elements, "Elements must not be null");
                this.prefix = prefix;
                this.methodName = methodName;
                this.params = new Object[elements.length];
                System.arraycopy(elements, 0, this.params, 0, elements.length);
                this.hashCode = prefix.hashCode();
                this.hashCode = 31 * this.hashCode + methodName.hashCode();
                this.hashCode = 31 * this.hashCode + Arrays.deepHashCode(this.params);
            }

            public boolean equals(Object other) {
                return this == other || other instanceof PrefixedSimpleKey && this.prefix.equals(((PrefixedSimpleKey)other).prefix) && this.methodName.equals(((PrefixedSimpleKey)other).methodName) && Arrays.deepEquals(this.params, ((PrefixedSimpleKey)other).params);
            }

            public final int hashCode() {
                return this.hashCode;
            }

            public String toString() {
                return this.prefix + " " + this.getClass().getSimpleName() + this.methodName + " [" + StringUtils.arrayToCommaDelimitedString(this.params) + "]";
            }
        }
    }
}
