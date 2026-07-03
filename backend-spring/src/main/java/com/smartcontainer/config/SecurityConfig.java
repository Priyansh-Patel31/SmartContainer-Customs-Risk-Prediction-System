package com.smartcontainer.config;

import com.smartcontainer.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration — JWT-based stateless auth with CORS.
 * Mirrors the existing Node.js auth middleware + CORS setup.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/docs/**", "/docs.json", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Admin-only endpoints
                        .requestMatchers("/api/auth/register").hasRole("ADMIN")
                        .requestMatchers("/api/auth/users").hasRole("ADMIN")
                        .requestMatchers("/api/auth/users/*/active").hasRole("ADMIN")

                        // Officer+ endpoints
                        .requestMatchers("/api/queue").hasAnyRole("ADMIN", "OFFICER")
                        .requestMatchers("/api/containers/*/assign").hasAnyRole("ADMIN", "OFFICER")
                        .requestMatchers("/api/containers/*/status").hasAnyRole("ADMIN", "OFFICER")
                        .requestMatchers("/api/containers/*/notes").hasAnyRole("ADMIN", "OFFICER")
                        .requestMatchers("/api/jobs").hasAnyRole("ADMIN", "OFFICER")
                        .requestMatchers("/api/report/summary.csv").hasAnyRole("ADMIN", "OFFICER")
                        .requestMatchers("/api/report/summary.pdf").hasAnyRole("ADMIN", "OFFICER")
                        .requestMatchers("/api/tracking/**").hasAnyRole("ADMIN", "OFFICER")
                        .requestMatchers("/api/exporters/**").hasAnyRole("ADMIN", "OFFICER")

                        // All authenticated users
                        .requestMatchers("/api/**").authenticated()

                        // Everything else
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList(
                "X-Prediction-Total", "X-Prediction-Critical",
                "X-Prediction-LowRisk", "X-Prediction-Clear",
                "X-Prediction-AutoEscalated", "Content-Disposition"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
