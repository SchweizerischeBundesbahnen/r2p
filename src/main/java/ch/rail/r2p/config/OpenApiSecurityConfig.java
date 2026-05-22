package ch.rail.r2p.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiSecurityConfig {

    @Value("${r2p.azure.issuer-uri}")
    private String issuerUri;
    @Value("${r2p.azure.client-secret}")
    private String clientSecret; // not used in this config, but present to make sure an error is raised at startup if the environment variable is missing

    @Value("${r2p.openapi.auth-url:}")
    private String configuredAuthUrl;

    @Value("${r2p.openapi.token-url:}")
    private String configuredTokenUrl;

    @Bean
    public OpenAPI openAPI() {
        String authUrl = configuredAuthUrl.isBlank()
            ? issuerUri + "/oauth2/v2.0/authorize"
            : configuredAuthUrl;
        String tokenUrl = configuredTokenUrl.isBlank()
            ? issuerUri + "/oauth2/v2.0/token"
            : configuredTokenUrl;

        OAuthFlow authorizationCodeFlow = new OAuthFlow()
                .authorizationUrl(authUrl)
                .tokenUrl(tokenUrl);

        OAuthFlows flows = new OAuthFlows().authorizationCode(authorizationCodeFlow);

        SecurityScheme oauthScheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(flows);

        // this ensures that the "authorize" button appears in swagger-ui
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("oauth2", oauthScheme))
                .addSecurityItem(new SecurityRequirement().addList("oauth2"));
    }
}
