package ru.creditbank.credit.operations.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Отдельный бин, чтобы не создавать цикл SecurityConfig -> GatewayAuthenticationFilter
 * -> SecurityContextRepository -> (обратно) SecurityConfig.
 */
@Configuration
public class SecurityContextConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new RequestAttributeSecurityContextRepository();
    }
}
