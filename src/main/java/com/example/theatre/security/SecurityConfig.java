package com.example.theatre.security;

import com.example.theatre.dto.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

    private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/app.css", "/app.js").permitAll()
                        .requestMatchers("/actuator/health", "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/movies/**", "/api/theatres/**", "/api/shows/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/bookings/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/payments/*/confirm-cash").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/payments/*/refund-cash").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/payments/admin/*").hasRole("ADMIN")
                        .requestMatchers("/api/payments/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/screens/**", "/api/seats/**").permitAll()
                        .requestMatchers("/api/movies/**", "/api/theatres/**", "/api/shows/**",
                                "/api/screens/**", "/api/seats/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, 401, "Unauthorized", "Authentication is required or the token is invalid",
                                        request.getRequestURI()))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, 403, "Forbidden", "You do not have permission to access this resource",
                                        request.getRequestURI())))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, 401, "Unauthorized", "Authentication is required or the token is invalid",
                                        request.getRequestURI()))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, 403, "Forbidden", "You do not have permission to access this resource",
                                        request.getRequestURI())))
                .build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    private void writeError(HttpServletResponse response, int status, String error,
                            String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = new ApiError(LocalDateTime.now(), status, error, message, path, Map.of());
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
