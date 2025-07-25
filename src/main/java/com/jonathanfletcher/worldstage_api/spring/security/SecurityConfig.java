package com.jonathanfletcher.worldstage_api.spring.security;

import com.jonathanfletcher.worldstage_api.property.CorsProperties;
import com.jonathanfletcher.worldstage_api.spring.security.model.ERole;
import com.jonathanfletcher.worldstage_api.spring.security.service.JwtAuthenticationFilter;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CorsProperties corsProperties;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    @PostConstruct
    public void init() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/streams/**", "/ws/**").permitAll()
                        .requestMatchers("/admin/**").hasRole(ERole.ADMIN.toString())
                        .requestMatchers("/mock/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
//                .csrf(csrf -> csrf
//                        .csrfTokenRepository(new StatelessCsrfTokenRepository())
//                        .ignoringRequestMatchers("/auth/login", "/auth/register")
//                )
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                    cors.configurationSource(corsConfigurationSource());
                })
//                .headers(headers -> headers
//                        .contentSecurityPolicy(csp ->
//                                csp.policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'")
//                        )
//                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // Explicitly bypass JWT filter for WebSocket endpoints (May be needed for websocket auth)
//                .addFilterBefore(new Filter() {
//                    @Override
//                    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//                            throws IOException, ServletException {
//                        HttpServletRequest httpRequest = (HttpServletRequest) request;
//                        if (httpRequest.getRequestURI().startsWith("/ws")) {
//                            chain.doFilter(request, response); // Bypass JWT filter
//                            return;
//                        }
//                        jwtFilter.doFilter(request, response, chain);
//                    }
//                }, JwtAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        log.info("CORS is enabled, configuring with allowed origins: {}", corsProperties.getAllowedOrigins());

        corsProperties.getAllowedOrigins().forEach(configuration::addAllowedOrigin);
        configuration.addAllowedMethod("*");
        corsProperties.getAllowedHeaders().forEach(configuration::addAllowedHeader);
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new
                UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}