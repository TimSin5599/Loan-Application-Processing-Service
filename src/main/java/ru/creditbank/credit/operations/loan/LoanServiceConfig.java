package ru.creditbank.credit.operations.loan;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.creditbank.credit.operations.logging.TraceIdPropagationInterceptor;

@Configuration
@EnableConfigurationProperties(LoanServiceProperties.class)
public class LoanServiceConfig {
    @Bean
    public RestClient loanServiceRestClient(RestClient.Builder builder,
                                             LoanServiceProperties properties,
                                             TraceIdPropagationInterceptor traceIdPropagationInterceptor) {
        return builder.baseUrl(properties.baseUrl())
                .requestInterceptor(traceIdPropagationInterceptor)
                .build();
    }
}
