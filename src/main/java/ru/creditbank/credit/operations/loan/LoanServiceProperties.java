package ru.creditbank.credit.operations.loan;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.loan-management")
public record LoanServiceProperties(String baseUrl, String internalApiKey) {
}
