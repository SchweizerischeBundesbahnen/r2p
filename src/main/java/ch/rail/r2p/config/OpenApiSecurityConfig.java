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

    @Bean
    public OpenAPI openAPI() {
        String authUrl = issuerUri + "/oauth2/v2.0/authorize";
        String tokenUrl = issuerUri + "/oauth2/v2.0/token";

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
