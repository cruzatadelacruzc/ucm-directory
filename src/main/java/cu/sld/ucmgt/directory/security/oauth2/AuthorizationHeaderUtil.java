package cu.sld.ucmgt.directory.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AuthorizationHeaderUtil {

    private final RestTemplateBuilder restTemplateBuilder;
    private final OAuth2AuthorizedClientService clientService;

    public AuthorizationHeaderUtil(RestTemplateBuilder restTemplateBuilder, OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
        this.restTemplateBuilder = restTemplateBuilder;
    }


    public Optional<String> getAuthorizationHeader() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            String name = oauthToken.getName();
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();
            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(registrationId, name);

            if (null == client) {
                throw new OAuth2AuthorizationException(new OAuth2Error("access_denied", "The token is expired", null));
            }

            OAuth2AccessToken accessToken = client.getAccessToken();
            if (accessToken != null) {
                String tokenType = accessToken.getTokenType().getValue();
                String accessTokenValue = accessToken.getTokenValue();
                if (isExpired(accessToken)) {
                    log.info("AccessToken expired, refreshing automatically");
                    accessTokenValue = refreshToken(client, oauthToken);
                    if (null == accessTokenValue) {
                        SecurityContextHolder.getContext().setAuthentication(null);
                        throw new OAuth2AuthorizationException(new OAuth2Error("access_denied", "The token is expired", null));
                    }
                }
                String authorizationHeaderValue = String.format("%s %s", tokenType, accessTokenValue);
                return Optional.of(authorizationHeaderValue);
            }
        }
        return Optional.empty();
    }

    private String refreshToken(OAuth2AuthorizedClient client, OAuth2AuthenticationToken oauthToken) {
        OAuth2AccessTokenResponse tokenResponse = refreshTokenClient(client);
        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            log.info("Failed to refresh token for user");
            return null;
        }

        OAuth2RefreshToken refreshToken = tokenResponse.getRefreshToken() != null ? tokenResponse.getRefreshToken() : client.getRefreshToken();
        OAuth2AuthorizedClient updatedClient = new OAuth2AuthorizedClient(
                client.getClientRegistration(),
                client.getPrincipalName(),
                tokenResponse.getAccessToken(),
                refreshToken
        );

        clientService.saveAuthorizedClient(updatedClient, oauthToken);
        return tokenResponse.getAccessToken().getTokenValue();
    }

    private OAuth2AccessTokenResponse refreshTokenClient(OAuth2AuthorizedClient client) {
        MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
        formParams.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue());
        formParams.add(OAuth2ParameterNames.REFRESH_TOKEN, client.getRefreshToken().getTokenValue());
        formParams.add(OAuth2ParameterNames.CLIENT_ID, client.getClientRegistration().getClientId());
        RequestEntity<?> requestEntity = RequestEntity
                .post(URI.create(client.getClientRegistration().getProviderDetails().getTokenUri()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formParams);
        try {
            RestTemplate rt = restTemplate(
                    client.getClientRegistration().getClientId(),
                    client.getClientRegistration().getClientSecret()
            );
            ResponseEntity<OAuthIdpTokenResponseDTO> responseEntity = rt.exchange(requestEntity, OAuthIdpTokenResponseDTO.class);
            return toOAuth2AccessTokenResponse(responseEntity.getBody());
        }  catch (OAuth2AuthorizationException e) {
            log.error("Unable to refresh token", e);
            throw new OAuth2AuthenticationException(e.getError(), e);
        }
    }

    private OAuth2AccessTokenResponse toOAuth2AccessTokenResponse(OAuthIdpTokenResponseDTO body) {
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("id_token", body.getIdToken());
        additionalParameters.put("not-before-policy", body.getNotBefore());
        additionalParameters.put("refresh_expires_in", body.getRefreshExpiresIn());
        additionalParameters.put("session_state", body.getSessionState());
        return OAuth2AccessTokenResponse
                .withToken(body.getAccessToken())
                .expiresIn(body.getExpiresIn())
                .refreshToken(body.getRefreshToken())
                .scopes(Pattern.compile("\\s").splitAsStream(body.getScope()).collect(Collectors.toSet()))
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .additionalParameters(additionalParameters)
                .build();
    }


    private RestTemplate restTemplate(String clientId, String clientSecret) {
        return restTemplateBuilder
                .additionalMessageConverters(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter())
                .errorHandler(new OAuth2ErrorResponseErrorHandler())
                .basicAuthentication(clientId, clientSecret)
                .build();
    }

    private boolean isExpired(OAuth2AccessToken accessToken) {
        Instant now = Instant.now();
        Instant expiresAt = accessToken.getExpiresAt();
        if (expiresAt != null) {
            return now.isAfter(expiresAt.minus(Duration.ofMinutes(1L)));
        }
        return false;
    }
}
