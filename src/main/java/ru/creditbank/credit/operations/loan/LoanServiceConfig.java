package ru.creditbank.credit.operations.loan;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LoanServiceProperties.class)
public class LoanServiceConfig {

    @Bean
    public RestClient loanServiceRestClient(RestClient.Builder builder, LoanServiceProperties properties) {
        return builder.baseUrl(properties.baseUrl()).build();
    }
}
