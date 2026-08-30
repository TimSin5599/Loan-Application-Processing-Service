package ru.creditbank.credit.operations.credit.scoring.service.rules;

import org.junit.jupiter.api.Test;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.scoring.service.RuleOutcome;
import ru.creditbank.credit.operations.credit.scoring.service.RuleVerdict;
import ru.creditbank.credit.operations.credit.scoring.service.ScoringContext;
import ru.creditbank.credit.operations.loan.dto.PaymentHistoryResponse;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DebtLoadRuleTest {
    private final DebtLoadRule rule = new DebtLoadRule();

    @Test
    void evaluate_noCreditHistory_abstains() {
        PaymentHistoryResponse history = new PaymentHistoryResponse(0, 0, false, BigDecimal.ZERO);

        RuleOutcome outcome = rule.evaluate(context(BigDecimal.valueOf(500_000), history));

        assertThat(outcome.verdict()).isEqualTo(RuleVerdict.ABSTAIN);
    }

    @Test
    void evaluate_projectedDebtWithinLimit_passes() {
        PaymentHistoryResponse history = new PaymentHistoryResponse(2, 1, false, BigDecimal.valueOf(500_000));

        RuleOutcome outcome = rule.evaluate(context(BigDecimal.valueOf(500_000), history));

        assertThat(outcome.verdict()).isEqualTo(RuleVerdict.PASS);
    }

    @Test
    void evaluate_projectedDebtAboveLimit_fails() {
        PaymentHistoryResponse history = new PaymentHistoryResponse(2, 2, false, BigDecimal.valueOf(4_800_000));

        RuleOutcome outcome = rule.evaluate(context(BigDecimal.valueOf(500_000), history));

        assertThat(outcome.verdict()).isEqualTo(RuleVerdict.FAIL);
        assertThat(outcome.reason()).isNotBlank();
    }

    private ScoringContext context(BigDecimal amount, PaymentHistoryResponse history) {
        CreditEntity credit = CreditEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .requestedAmount(amount)
                .termMonths(12)
                .status(CreditStatus.PENDING)
                .build();
        return new ScoringContext(credit, history);
    }
}
