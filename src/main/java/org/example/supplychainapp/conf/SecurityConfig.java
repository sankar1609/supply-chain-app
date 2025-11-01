package org.example.supplychainapp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.allowed.paths:/api/auth/**,/swagger-ui/**,/v3/api-docs/**}")
    private String[] allowedPaths;

    /*private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }
*/
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF since this is a REST API
                .csrf(csrf -> csrf.disable())
                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Make session stateless (JWT-based)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Define which requests are allowed without authentication
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(allowedPaths).permitAll()
                        .requestMatchers("/assets/queryLogByProductId/**").hasAnyRole("ADMIN","USER")
                        .requestMatchers( "/assets/queryProduct/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers( "/assets/update/**").hasRole("ADMIN")
                        .requestMatchers( "/assets/createProduct/**").hasRole("ADMIN")
                        .requestMatchers( "/assets/removeProduct/**").hasRole("ADMIN")
                        .requestMatchers( "/assets/createShipment/**").hasRole("ADMIN")
                        .requestMatchers( "/assets/queryShipment/**").hasAnyRole("ADMIN","USER")
                        .requestMatchers( "/assets/queryLogByProductId/**").hasAnyRole("ADMIN","USER")
                        .requestMatchers( "/assets/updateShipment/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );
                // Add JWT filter before UsernamePasswordAuthenticationFilter
                //.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000")); // your frontend URL
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
