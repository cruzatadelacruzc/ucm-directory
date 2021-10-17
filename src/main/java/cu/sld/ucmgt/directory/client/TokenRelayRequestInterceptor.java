package cu.sld.ucmgt.directory.client;

import cu.sld.ucmgt.directory.security.oauth2.AuthorizationHeaderUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.util.Optional;

public class TokenRelayRequestInterceptor implements RequestInterceptor {

    public static final String AUTHORIZATION = "Authorization";

    private final AuthorizationHeaderUtil authorizationHeaderUtil;

    public TokenRelayRequestInterceptor(AuthorizationHeaderUtil authorizationHeaderUtil) {
        super();
        this.authorizationHeaderUtil = authorizationHeaderUtil;
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        Optional<String> authorizationHeader = authorizationHeaderUtil.getAuthorizationHeader();
    }
}
