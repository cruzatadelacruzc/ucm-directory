package cu.sld.ucmgt.directory.config;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

import java.net.URI;

@Slf4j
@Configuration
@Profile(value = Constants.PROFILE_PROD)
public class RestClientConfig extends AbstractElasticsearchConfiguration {


    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {
        final String stringUrl = System.getenv("SPRING_ELASTICSEARCH_REST_URIS");
        final URI uri = URI.create(stringUrl);

        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 9200 : uri.getPort();
        final ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder().connectedTo(host + ":" + port);

        // enable SSL if https is being used in the URL
        boolean isSsl = "https".equals(uri.getScheme());
        if (isSsl) {
            builder.usingSsl();
        }

        // enable basic auth if specified
        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            final String[] userPass = userInfo.split(":", 2);
            builder.withBasicAuth(userPass[0], userPass[1]);
        }

        log.info("Elasticsearch server [{}:{}] ssl[{}] auth[{}]", host, port, isSsl, userInfo != null);
        return RestClients.create(builder.build()).rest();
    }
}
