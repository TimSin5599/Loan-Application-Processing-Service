package ru.creditbank.credit.operations.credit.scoring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.credit.manage.service.CreditAlreadyDecidedException;
import ru.creditbank.credit.operations.credit.manage.service.CreditDecisionService;
import ru.creditbank.credit.operations.credit.manage.service.LoanApprovalSaga;
import ru.creditbank.credit.operations.exception.CreditNotFoundException;
import ru.creditbank.credit.operations.loan.PaymentHistoryClient;
import ru.creditbank.credit.operations.loan.PaymentHistoryUnavailableException;
import ru.creditbank.credit.operations.loan.dto.PaymentHistoryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CreditScoringService {
    private static final Logger log = LoggerFactory.getLogger(CreditScoringService.class);

    static final BigDecimal AUTO_APPROVAL_INTEREST_RATE = BigDecimal.valueOf(14.9);
    private static final String AUTO_APPROVAL_COMMENT = "Заявка одобрена автоматически по результатам скоринга";

    private final CreditProvider creditProvider;
    private final PaymentHistoryClient paymentHistoryClient;
    private final CreditDecisionService creditDecisionService;
    private final LoanApprovalSaga loanApprovalSaga;
    private final List<ScoringRule> rules;

    public CreditScoringService(CreditProvider creditProvider,
                                 PaymentHistoryClient paymentHistoryClient,
                                 CreditDecisionService creditDecisionService,
                                 LoanApprovalSaga loanApprovalSaga,
                                 List<ScoringRule> rules) {
        this.creditProvider = creditProvider;
        this.paymentHistoryClient = paymentHistoryClient;
        this.creditDecisionService = creditDecisionService;
        this.loanApprovalSaga = loanApprovalSaga;
        this.rules = rules;
    }

    @Async("scoringExecutor")
    public void scoreApplication(UUID creditId) {
        log.info("Запущен автоматический скоринг заявки creditId={}", creditId);
        CreditEntity credit = creditProvider.findById(creditId)
                .orElseThrow(() -> new CreditNotFoundException(creditId));

        if (credit.getStatus() != CreditStatus.PENDING) {
            return;
        }

        PaymentHistoryResponse history;
        try {
            history = paymentHistoryClient.fetchHistory(credit.getUserId());
        } catch (PaymentHistoryUnavailableException e) {
            log.warn("Кредитная история недоступна, заявка {} передана на ручную проверку", creditId, e);
            return;
        }

        List<RuleOutcome> outcomes = rules.stream()
                .map(rule -> rule.evaluate(new ScoringContext(credit, history)))
                .toList();

        if (outcomes.stream().anyMatch(outcome -> outcome.verdict() == RuleVerdict.FAIL)) {
            String reason = outcomes.stream()
                    .filter(outcome -> outcome.verdict() == RuleVerdict.FAIL)
                    .map(RuleOutcome::reason)
                    .collect(Collectors.joining("; "));
            log.info("Скоринг отклонил заявку creditId={} reason={}", creditId, reason);
            rejectIfStillPending(creditId, reason);
            return;
        }

        if (outcomes.stream().anyMatch(outcome -> outcome.verdict() == RuleVerdict.ABSTAIN)) {
            log.info("Заявка {} требует ручной проверки менеджером", creditId);
            return;
        }

        approveIfStillPending(creditId);
    }

    private void rejectIfStillPending(UUID creditId, String reason) {
        try {
            creditDecisionService.reject(creditId, reason);
        } catch (CreditAlreadyDecidedException e) {
            log.info("Заявка {} уже обработана к моменту завершения скоринга, автоматическое решение проигнорировано", creditId);
        }
    }

    private void approveIfStillPending(UUID creditId) {
        try {
            loanApprovalSaga.approve(creditId, AUTO_APPROVAL_COMMENT, AUTO_APPROVAL_INTEREST_RATE);
        } catch (CreditAlreadyDecidedException e) {
            log.info("Заявка {} уже обработана к моменту завершения скоринга, автоматическое решение проигнорировано", creditId);
        }
    }
}
