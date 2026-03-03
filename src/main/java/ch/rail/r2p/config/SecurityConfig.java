package ch.rail.r2p.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        // TODO: re-enable security – temporarily disabled to allow UI development without OAuth2 token
        // WebExpressionAuthorizationManager scopeAndRole = new WebExpressionAuthorizationManager("hasAuthority('SCOPE_r2p') and hasRole('r2p_user')");
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            // TODO: re-enable OAuth2 resource server when security is restored (replace disable() with jwt config below)
            // .oauth2ResourceServer(oauth2 -> oauth2
            //     .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            // );
            .oauth2ResourceServer(oauth2 -> oauth2.disable());
        return http.build();
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
