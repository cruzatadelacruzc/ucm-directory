package cu.sld.ucmgt.directory.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

import java.util.List;

@Slf4j
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {
    private final List<String> allowedAudience;

    public AudienceValidator(List<String> audiences) {
        Assert.notEmpty(audiences, "Allowed audience should not be null or empty.");
        this.allowedAudience = audiences;
    }


    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audience = jwt.getAudience();
        if (audience.stream().anyMatch(allowedAudience::contains)) {
            return OAuth2TokenValidatorResult.success();
        } else {
            log.warn("Invalid audience: {}", audience);
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "The required audience is missing", null
            ));
        }
    }
}
