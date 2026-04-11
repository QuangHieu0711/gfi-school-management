package com.gfi.backend.models.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.gfi.backend.models.security.JwtAuthenticationFilter;
import com.gfi.backend.filters.UserScopesLoadingFilter;
import com.gfi.backend.repositories.UserRepository;
import com.gfi.backend.services.interfaces.AuthPermissionService;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;
        private final AuthPermissionService authPermissionService;
        private final UserRepository userRepository;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                // Create UserScopesLoadingFilter instance
                UserScopesLoadingFilter userScopesLoadingFilter = new UserScopesLoadingFilter(authPermissionService,
                                userRepository);

                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/login", "/api/auth/logout",
                                                                "/v3/api-docs/**", "/swagger-ui/**", "/uploads/**")
                                                .permitAll()
                                                .requestMatchers("/login", "/home", "/main", "/apartment", "/resident",
                                                                "/notification", "/feedback", "/invoice",
                                                                "/financial",
                                                                "/device",
                                                                "/maintenance", "/admin", "/css/**", "/js/**",
                                                                "/images/**", "/favicon.ico")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(userScopesLoadingFilter, JwtAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.asList(
                                "http://localhost:3000",
                                "http://127.0.0.1:3000",
                                "http://localhost:4200",
                                "https://localhost:4200",
                                "http://127.0.0.1:4200",
                                "https://127.0.0.1:4200"));
                configuration.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setExposedHeaders(Arrays.asList(
                                "Authorization", "Content-Type", "X-Requested-With"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}
