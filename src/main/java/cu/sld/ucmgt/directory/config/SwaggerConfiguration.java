package cu.sld.ucmgt.directory.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
public class SwaggerConfiguration {

    private final AppProperties properties;

    @Bean
    public Docket apiDocket() {
        Contact contact = new Contact(
                properties.getSwagger().getContactName(),
                properties.getSwagger().getContactUrl(),
                properties.getSwagger().getContactEmail()
        );

        ApiInfo apiInfo = new ApiInfo(
                properties.getSwagger().getTitle(),
                properties.getSwagger().getDescription(),
                properties.getSwagger().getVersion(),
                properties.getSwagger().getTermsOfServiceUrl(),
                contact,
                properties.getSwagger().getLicense(),
                properties.getSwagger().getLicenseUrl(),
                new ArrayList<>()
        );
        return new Docket(DocumentationType.SWAGGER_2)
                .host(properties.getSwagger().getHost())
                .protocols(new HashSet<>(Arrays.asList(properties.getSwagger().getProtocols())))
                .useDefaultResponseMessages(properties.getSwagger().isUseDefaultResponseMessages())
                .directModelSubstitute(ByteBuffer.class, String.class)
                .genericModelSubstitutes(new Class[]{ResponseEntity.class})
                .select().paths(PathSelectors.regex(this.properties.getSwagger().getDefaultIncludePattern()))
                .apis(RequestHandlerSelectors.basePackage("cu.sld.ucmgt.directory.web.rest"))
                .build()
                .apiInfo(apiInfo);
    }
}
