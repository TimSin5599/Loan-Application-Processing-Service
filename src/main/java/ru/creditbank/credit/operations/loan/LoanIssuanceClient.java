package ru.creditbank.credit.operations.loan;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.loan.dto.CancelLoanResponse;
import ru.creditbank.credit.operations.loan.dto.CreatePaymentScheduleResponse;
import ru.creditbank.credit.operations.loan.dto.IssueLoanRequest;
import ru.creditbank.credit.operations.loan.dto.IssueLoanResponse;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class LoanIssuanceClient {
    private static final Logger log = LoggerFactory.getLogger(LoanIssuanceClient.class);

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String LOANS_PATH = "/loan-management-service/internal/loans";
    private static final String SCHEDULE_PATH = "/loan-management-service/internal/loans/{loanId}/schedule";
    private static final String CANCEL_PATH = "/loan-management-service/internal/loans/{loanId}/cancel";

    private final RestClient loanServiceRestClient;
    private final LoanServiceProperties properties;

    public LoanIssuanceClient(RestClient loanServiceRestClient, LoanServiceProperties properties) {
        this.loanServiceRestClient = loanServiceRestClient;
        this.properties = properties;
    }

    @Retry(name = "loanManagementService")
    @CircuitBreaker(name = "loanManagementService")
    public UUID issueLoan(CreditEntity credit) {
        IssueLoanRequest request = new IssueLoanRequest(
                credit.getId(),
                credit.getUserId(),
                credit.getRequestedAmount(),
                credit.getTermMonths(),
                credit.getInterestRate()
        );

        IssueLoanResponse response = loanServiceRestClient.post()
                .uri(LOANS_PATH)
                .header(API_KEY_HEADER, properties.internalApiKey())
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey(credit.getId(), "issue"))
                .body(request)
                .retrieve()
                .body(IssueLoanResponse.class);

        log.info("Кредит выдан по заявке creditId={} loanId={}", credit.getId(), response.loanId());
        return response.loanId();
    }

    @Retry(name = "loanManagementService")
    @CircuitBreaker(name = "loanManagementService")
    public void createPaymentSchedule(UUID creditId, UUID loanId) {
        CreatePaymentScheduleResponse response = loanServiceRestClient.post()
                .uri(SCHEDULE_PATH, loanId)
                .header(API_KEY_HEADER, properties.internalApiKey())
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey(creditId, "schedule"))
                .retrieve()
                .body(CreatePaymentScheduleResponse.class);

        log.info("Создан график платежей creditId={} loanId={} installments={}",
                creditId, loanId, response.installmentsCreated());
    }

    @Retry(name = "loanManagementService")
    @CircuitBreaker(name = "loanManagementService")
    public void cancelLoan(UUID creditId, UUID loanId) {
        CancelLoanResponse response = loanServiceRestClient.post()
                .uri(CANCEL_PATH, loanId)
                .header(API_KEY_HEADER, properties.internalApiKey())
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey(creditId, "cancel"))
                .retrieve()
                .body(CancelLoanResponse.class);

        log.info("Кредит отменён (компенсация) creditId={} loanId={} status={}",
                creditId, loanId, response.status());
    }

    private String idempotencyKey(UUID creditId, String step) {
        return UUID.nameUUIDFromBytes((creditId + ":" + step).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
