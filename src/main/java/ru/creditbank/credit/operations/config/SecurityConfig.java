package ru.creditbank.credit.operations.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;
    private final SecurityContextRepository securityContextRepository;

    public SecurityConfig(GatewayAuthenticationFilter gatewayAuthenticationFilter,
                           SecurityContextRepository securityContextRepository) {
        this.gatewayAuthenticationFilter = gatewayAuthenticationFilter;
        this.securityContextRepository = securityContextRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(
                                (request, response, authException) -> response.sendError(HttpStatus.UNAUTHORIZED.value()))
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> response.sendError(HttpStatus.FORBIDDEN.value())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/credit-service/api/credit/*/status")
                        .hasRole(Roles.CREDIT_MANAGER)
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}