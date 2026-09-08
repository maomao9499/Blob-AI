package dev.blob.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CorsConfig extends CorsFilter {
    public CorsConfig(@Value("${blob.desktop-token:}") String token,
                      @Value("${blob.dev-origin:http://127.0.0.1:5173}") String devOrigin,
                      @Value("${blob.allow-dev-origin:false}") boolean allowDevOrigin) {
        super(source(token, devOrigin, allowDevOrigin));
    }

    private static UrlBasedCorsConfigurationSource source(String token, String devOrigin, boolean allowDevOrigin) {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("blob-app://app");
        if ((token.isEmpty() || allowDevOrigin) && !devOrigin.isBlank()) {
            if (!devOrigin.matches("http://127\\.0\\.0\\.1:[0-9]{1,5}")) {
                throw new IllegalArgumentException("Development origin must be an exact loopback HTTP origin");
            }
            config.addAllowedOrigin(devOrigin);
        }
        config.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-Blob-Desktop-Token", "X-Trace-Id"));
        config.setExposedHeaders(List.of("X-Trace-Id"));
        config.setMaxAge(600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", config);
        return source;
    }
}
