package ru.creditbank.credit.operations.credit.manage.service;

import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.loan.LoanIssuanceClient;

import java.math.BigDecimal;

/**
 * Единая точка применения решения по заявке — используется и при ручном
 * решении менеджера, и при автоматическом скоринге, чтобы не дублировать
 * логику выдачи кредита и нотификации.
 */
@Service
public class CreditDecisionService {

    private final CreditProvider creditProvider;
    private final CreditNotificationService creditNotificationService;
    private final LoanIssuanceClient loanIssuanceClient;

    public CreditDecisionService(CreditProvider creditProvider,
                                  CreditNotificationService creditNotificationService,
                                  LoanIssuanceClient loanIssuanceClient) {
        this.creditProvider = creditProvider;
        this.creditNotificationService = creditNotificationService;
        this.loanIssuanceClient = loanIssuanceClient;
    }

    public CreditEntity applyDecision(CreditEntity credit, CreditStatus status, String comment, BigDecimal interestRate) {
        if (interestRate != null) {
            credit.setInterestRate(interestRate);
        }

        if (status == CreditStatus.APPROVED) {
            loanIssuanceClient.issueLoan(credit);
        }

        credit.setStatus(status);
        credit.setManagerComment(comment);

        CreditEntity saved = creditProvider.save(credit);
        creditNotificationService.notifyStatusChange(saved);
        return saved;
    }
}
