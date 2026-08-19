package ru.creditbank.credit.operations.credit.scoring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.credit.manage.service.CreditDecisionService;
import ru.creditbank.credit.operations.exception.CreditNotFoundException;
import ru.creditbank.credit.operations.loan.PaymentHistoryClient;
import ru.creditbank.credit.operations.loan.PaymentHistoryUnavailableException;
import ru.creditbank.credit.operations.loan.dto.PaymentHistoryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Автоматический скоринг заявки: выполняется в фоне после создания заявки,
 * не блокируя ответ клиенту. Итог:
 *  - хотя бы одно правило FAIL  -> заявка отклоняется автоматически;
 *  - нет FAIL, но есть ABSTAIN  -> заявка остаётся PENDING, нужна ручная проверка менеджером;
 *  - все правила PASS           -> заявка одобряется автоматически.
 */
@Service
public class CreditScoringService {

    private static final Logger log = LoggerFactory.getLogger(CreditScoringService.class);

    static final BigDecimal AUTO_APPROVAL_INTEREST_RATE = BigDecimal.valueOf(14.9);
    private static final String AUTO_APPROVAL_COMMENT = "Заявка одобрена автоматически по результатам скоринга";

    private final CreditProvider creditProvider;
    private final PaymentHistoryClient paymentHistoryClient;
    private final CreditDecisionService creditDecisionService;
    private final List<ScoringRule> rules;

    public CreditScoringService(CreditProvider creditProvider,
                                 PaymentHistoryClient paymentHistoryClient,
                                 CreditDecisionService creditDecisionService,
                                 List<ScoringRule> rules) {
        this.creditProvider = creditProvider;
        this.paymentHistoryClient = paymentHistoryClient;
        this.creditDecisionService = creditDecisionService;
        this.rules = rules;
    }

    @Async("scoringExecutor")
    public void scoreApplication(UUID creditId) {
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
            creditDecisionService.applyDecision(credit, CreditStatus.REJECTED, reason, null);
            return;
        }

        if (outcomes.stream().anyMatch(outcome -> outcome.verdict() == RuleVerdict.ABSTAIN)) {
            log.info("Заявка {} требует ручной проверки менеджером", creditId);
            return;
        }

        creditDecisionService.applyDecision(credit, CreditStatus.APPROVED, AUTO_APPROVAL_COMMENT, AUTO_APPROVAL_INTEREST_RATE);
    }
}
