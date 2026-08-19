package ru.creditbank.credit.operations.loan;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.loan.dto.IssueLoanRequest;

@Component
public class LoanIssuanceClient {

    private static final String ISSUE_LOAN_PATH = "/loan-management-service/internal/loans";
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient loanServiceRestClient;
    private final LoanServiceProperties properties;

    public LoanIssuanceClient(RestClient loanServiceRestClient, LoanServiceProperties properties) {
        this.loanServiceRestClient = loanServiceRestClient;
        this.properties = properties;
    }

    public void issueLoan(CreditEntity credit) {
        IssueLoanRequest request = new IssueLoanRequest(
                credit.getId(),
                credit.getUserId(),
                credit.getRequestedAmount(),
                credit.getTermMonths(),
                credit.getInterestRate()
        );

        try {
            loanServiceRestClient.post()
                    .uri(ISSUE_LOAN_PATH)
                    .header(API_KEY_HEADER, properties.internalApiKey())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new LoanIssuanceFailedException(credit.getId(), e);
        }
    }
}
