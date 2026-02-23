package com.lockerroom.resourceservice.configuration;

import com.lockerroom.resourceservice.security.KeycloakRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/notices/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sports/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/boards/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/{postId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/{postId}/comments").permitAll()
                        // Swagger & tools
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/h2-console/**").permitAll()
                        .requestMatchers("/api/name", "/api/version").permitAll()
                        // Admin
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Everything else requires authentication
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }
}
