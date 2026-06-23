package ch.rail.r2p.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${r2p.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Custom AuthorizationManager that handles two authentication mechanisms:
        //
        //  1. JWT Bearer token (REST API / Swagger-UI):
        //     The request authenticated as a JwtAuthenticationToken (oauth2ResourceServer).
        //     Requires both SCOPE_r2p (API consent) and ROLE_r2p_user (AD group membership).
        //
        //  2. Session cookie (browser OIDC login):
        //     Any other (non-JWT) authentication. Azure AD ID tokens do not carry an 'scp' claim,
        //     so only ROLE_r2p_user is required.
        //
        // The decision keys off the authentication type rather than the Authorization header so it
        // stays consistent with how the request was actually authenticated.
        AuthorizationManager<RequestAuthorizationContext> roleAndScope = (authSupplier, ctx) -> {
            var auth = authSupplier.get();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return new AuthorizationDecision(false);
            }
            Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
            boolean hasRole = authorities.contains("ROLE_r2p_user");
            // Bearer-token (JWT) requests must also carry the API scope.
            boolean granted = (auth instanceof JwtAuthenticationToken)
                ? hasRole && authorities.contains("SCOPE_r2p")
                : hasRole;
            return new AuthorizationDecision(granted);
        };
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                // CSRF protection is enforced for session-authenticated (browser) requests,
                // but ignored for stateless Bearer token API calls.
                .ignoringRequestMatchers(SecurityConfig::isBearerRequest)
            )
            // Sessions are needed for the browser-based OAuth2 login flow.
            // REST API clients (e.g. Swagger-UI) authenticate stateless via JWT Bearer token.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // Static frontend resources – always public
                .requestMatchers("/", "/*.html", "/style.css", "/favicon.ico").permitAll()
                .requestMatchers("/js/**").permitAll()
                // Spring Boot / WebJars resources
                .requestMatchers("/webjars/**", "/actuator/health/**").permitAll()
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
            )
            // Unauthenticated requests to protected resources must return 401, never a redirect
            // to the IdP login page. Without this, oauth2Login's LoginUrlAuthenticationEntryPoint
            // issues a 302 to /oauth2/authorization/azure -> login.microsoftonline.com, which an
            // AJAX fetch follows cross-origin and the browser blocks with a CORS error (that origin
            // has no Access-Control-Allow-Origin header).
            //
            // This applies to ALL protected paths, not just /v1/**: the integrated UI loads its
            // public HTML pages, then fetches data via AJAX (/v1/** and /internal/**). On 401 the
            // frontend itself navigates to /oauth2/authorization/azure (see r2p-api.js / router.js),
            // which triggers the Azure redirect via OAuth2AuthorizationRequestRedirectFilter - a
            // separate mechanism that is unaffected by this entry point.
            //
            // Bearer-token requests still get the RFC 6750 "WWW-Authenticate: Bearer ..." challenge:
            // we delegate to BearerTokenAuthenticationEntryPoint for them and only fall back to the
            // plain 401 for session-cookie (browser) requests. Note: setting a single
            // authenticationEntryPoint replaces the configurer's delegating entry point entirely
            // (an explicit authenticationEntryPoint short-circuits createDefaultEntryPoint, so the
            // BearerTokenAuthenticationEntryPoint that oauth2ResourceServer auto-registers would
            // otherwise be dropped) - hence we delegate explicitly here.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(apiAuthenticationEntryPoint())
            );
        return http.build();
    }

    private static boolean isBearerRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return authHeader != null
            && authHeader.regionMatches(true, 0, "bearer ", 0, 7);
    }

    /**
     * Authentication entry point for unauthenticated requests to protected resources.
     * <p>
     * Bearer-token API requests are delegated to {@link BearerTokenAuthenticationEntryPoint} so the
     * RFC 6750 {@code WWW-Authenticate: Bearer ...} challenge (incl. {@code error}/
     * {@code error_description} for invalid/expired tokens) is preserved. All other (session-cookie
     * / browser) requests get a plain {@code 401}, never a {@code 302} redirect to the IdP — the
     * frontend turns that 401 into a login redirect itself.
     */
    private static AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        AuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
        AuthenticationEntryPoint statusEntryPoint = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        return (request, response, authException) ->
            (isBearerRequest(request) ? bearerEntryPoint : statusEntryPoint)
                .commence(request, response, authException);
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

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
