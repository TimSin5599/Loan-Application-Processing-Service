package ru.creditbank.credit.operations.loan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.creditbank.credit.operations.loan.dto.PaymentHistoryResponse;

import java.util.UUID;

@Component
public class PaymentHistoryClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentHistoryClient.class);

    private static final String PAYMENT_HISTORY_PATH = "/loan-management-service/internal/users/{userId}/payment-history";
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient loanServiceRestClient;
    private final LoanServiceProperties properties;

    public PaymentHistoryClient(RestClient loanServiceRestClient, LoanServiceProperties properties) {
        this.loanServiceRestClient = loanServiceRestClient;
        this.properties = properties;
    }

    public PaymentHistoryResponse fetchHistory(UUID userId) {
        try {
            PaymentHistoryResponse history = loanServiceRestClient.get()
                    .uri(PAYMENT_HISTORY_PATH, userId)
                    .header(API_KEY_HEADER, properties.internalApiKey())
                    .retrieve()
                    .body(PaymentHistoryResponse.class);
            log.info("Получена кредитная история пользователя userId={} totalLoans={}",
                    userId, history == null ? null : history.totalLoans());
            return history;
        } catch (RestClientException e) {
            throw new PaymentHistoryUnavailableException(userId, e);
        }
    }
}
