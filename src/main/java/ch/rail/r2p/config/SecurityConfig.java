package ch.rail.r2p.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Custom AuthorizationManager that handles two authentication paths:
        //
        //  1. JWT Bearer token (REST API / Swagger-UI):
        //     Detected by the presence of the "Authorization: Bearer ..." header.
        //     Requires both SCOPE_r2p (API consent) and ROLE_r2p_user (AD group membership).
        //
        //  2. Session cookie (browser OIDC login):
        //     No Bearer header present. Azure AD ID tokens do not carry an 'scp' claim,
        //     so only ROLE_r2p_user is required.
        AuthorizationManager<RequestAuthorizationContext> roleAndScope = (authSupplier, ctx) -> {
            var auth = authSupplier.get();
            if (auth == null || !auth.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }
            Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
            boolean hasRole = authorities.contains("ROLE_r2p_user");
            // Bearer token requests must also carry the API scope
            String authHeader = ctx.getRequest().getHeader("Authorization");
            boolean isBearerRequest = authHeader != null
                && authHeader.regionMatches(true, 0, "bearer ", 0, 7);
            boolean granted = isBearerRequest
                ? hasRole && authorities.contains("SCOPE_r2p")
                : hasRole;
            return new AuthorizationDecision(granted);
        };
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Sessions are needed for the browser-based OAuth2 login flow.
            // REST API clients (e.g. Swagger-UI) authenticate stateless via JWT Bearer token.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // Static frontend resources – always public
                .requestMatchers("/", "/index.html", "/details.html", "/style.css", "/favicon.ico").permitAll()
                // Spring Boot / WebJars resources
                .requestMatchers("/webjars/**", "/actuator/health").permitAll()
                // Swagger UI & OpenAPI spec – secured by their own OAuth2 flow inside the browser
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // OAuth2 / OIDC login endpoints (Spring Security)
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                // Everything else requires authentication
                .anyRequest().access(roleAndScope)
            )
            // Browser-based OIDC login (session cookie). A custom OidcUserService maps Azure AD
            // application roles from the ID token to ROLE_* granted authorities.
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/index.html", true)
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService()))
            )
            // Stateless JWT Bearer token validation for REST API clients (e.g. Swagger-UI, curl).
            // When a Bearer token is present in the Authorization header it takes precedence over
            // the session cookie.
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        return http.build();
    }

    /**
     * Custom OIDC user service for browser-based logins.
     * <p>
     * Azure AD includes application roles in the {@code roles} claim of the ID token.
     * Spring Security's default {@link OidcUserService} maps these only to plain string
     * authorities; this service additionally prefixes them with {@code ROLE_} so that
     * {@code hasRole('r2p_user')} works for session-cookie-based requests in the same
     * way as for Bearer-token requests handled by {@link #jwtAuthenticationConverter()}.
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();
        return (OidcUserRequest userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);

            // Extract application roles from the ID token 'roles' claim
            Collection<String> roles = Optional
                .ofNullable(oidcUser.getClaimAsStringList("roles"))
                .orElse(Collections.emptyList());

            Set<GrantedAuthority> mappedAuthorities = Stream.concat(
                    oidcUser.getAuthorities().stream(),
                    roles.stream()
                        .filter(r -> !r.isBlank())
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                )
                .collect(Collectors.toSet());

            return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }

    @Bean
    public Converter<@NonNull Jwt, @NonNull AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            // Azure AD 'scp' claim: space separated scopes
            Collection<String> scopes = Optional.ofNullable(jwt.getClaimAsString("scp"))
                .map(s -> Arrays.stream(s.split(" ")).filter(str -> !str.isBlank()).collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
            Set<String> scopeAuthorities = scopes.stream()
                .map(scope -> "SCOPE_" + scope)
                .collect(Collectors.toSet());

            // Azure AD 'roles' claim: list of application roles
            Collection<String> roles = Optional.ofNullable(jwt.getClaimAsStringList("roles"))
                .orElse(Collections.emptyList());
            Set<String> roleAuthorities = roles.stream()
                .filter(r -> !r.isBlank())
                .map(r -> "ROLE_" + r)
                .collect(Collectors.toSet());

            Set<SimpleGrantedAuthority> authorities = Stream.concat(scopeAuthorities.stream(), roleAuthorities.stream())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

            // Use preferred_username (or fallback to sub) as principal name
            String principalName = Optional.ofNullable(jwt.getClaimAsString("preferred_username"))
                .orElse(jwt.getSubject());

            return new JwtAuthenticationToken(jwt, authorities, principalName);
        };
    }
}
