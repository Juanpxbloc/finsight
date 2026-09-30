package com.finsight.finsight_api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS for the Next.js frontend.
 *
 * The bean is declared as UrlBasedCorsConfigurationSource on purpose: that is the
 * concrete type Spring Security looks for when it decides whether to apply its CORS
 * configurer. SecurityConfig also calls cors(Customizer.withDefaults()) explicitly,
 * so the CorsFilter is wired in regardless.
 *
 * Wiring CORS into the security chain (and not through WebMvcConfigurer) matters:
 * the CorsFilter runs before authorization, so it answers the preflight OPTIONS
 * itself. A browser never sends the Authorization header on a preflight, so with
 * CORS handled later in the chain every preflight would be rejected as a 401 by
 * anyRequest().authenticated() and the real request would never be sent.
 */
@Slf4j
@Configuration
public class CorsConfig {

    /**
     * Origins allowed to call the API from a browser. Comma separated, so extra
     * environments can be added without touching the code:
     *   cors.allowed-origins=http://localhost:3000,https://finsight.vercel.app
     *
     * These must be exact origins (scheme + host + port), not paths and not "*".
     */
    @Value("${cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    /** How long the browser may cache the preflight result, in seconds. */
    @Value("${cors.max-age-seconds:3600}")
    private Long maxAgeSeconds;

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Authorization must be allowed or the browser strips the Bearer token from
        // the real request. Content-Type covers both application/json and the
        // multipart/form-data of the CSV upload.
        config.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT));

        // A Bearer token travelling in a header needs no credentials support. Leaving
        // this false also keeps the configuration legal if an origin is ever widened.
        config.setAllowCredentials(false);

        config.setMaxAge(maxAgeSeconds);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        log.info("CORS enabled for /api/** from origins: {}", allowedOrigins);

        return source;
    }
}
