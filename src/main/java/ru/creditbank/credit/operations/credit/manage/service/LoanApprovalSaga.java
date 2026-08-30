package ru.creditbank.credit.operations.credit.manage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.loan.LoanIssuanceClient;
import ru.creditbank.credit.operations.loan.LoanIssuanceFailedException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class LoanApprovalSaga {
    private static final Logger log = LoggerFactory.getLogger(LoanApprovalSaga.class);

    private final CreditDecisionService creditDecisionService;
    private final LoanIssuanceClient loanIssuanceClient;

    public LoanApprovalSaga(CreditDecisionService creditDecisionService, LoanIssuanceClient loanIssuanceClient) {
        this.creditDecisionService = creditDecisionService;
        this.loanIssuanceClient = loanIssuanceClient;
    }

    public CreditEntity approve(UUID creditId, String comment, BigDecimal interestRate) {
        CreditEntity credit = creditDecisionService.beginApproval(creditId, comment, interestRate);

        UUID loanId;
        try {
            loanId = loanIssuanceClient.issueLoan(credit);
        } catch (Exception e) {
            creditDecisionService.revertFailedApproval(creditId, "Не удалось создать кредит: " + e.getMessage());
            throw new LoanIssuanceFailedException(creditId, e);
        }

        try {
            loanIssuanceClient.createPaymentSchedule(creditId, loanId);
        } catch (Exception e) {
            compensate(creditId, loanId);
            creditDecisionService.revertFailedApproval(creditId, "Не удалось создать график платежей: " + e.getMessage());
            throw new LoanIssuanceFailedException(creditId, e);
        }

        return creditDecisionService.completeApproval(creditId);
    }

    private void compensate(UUID creditId, UUID loanId) {
        try {
            loanIssuanceClient.cancelLoan(creditId, loanId);
        } catch (Exception cancelException) {
            log.error("КРИТИЧНО: не удалось скомпенсировать кредит loanId={} по заявке creditId={} — требуется ручное вмешательство",
                    loanId, creditId, cancelException);
        }
    }
}
