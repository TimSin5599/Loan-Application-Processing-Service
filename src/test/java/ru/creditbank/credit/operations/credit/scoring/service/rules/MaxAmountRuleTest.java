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

class MaxAmountRuleTest {

    private final MaxAmountRule rule = new MaxAmountRule();

    @Test
    void evaluate_amountWithinLimit_passes() {
        RuleOutcome outcome = rule.evaluate(context(BigDecimal.valueOf(1_000_000)));

        assertThat(outcome.verdict()).isEqualTo(RuleVerdict.PASS);
    }

    @Test
    void evaluate_amountAboveLimit_fails() {
        RuleOutcome outcome = rule.evaluate(context(BigDecimal.valueOf(3_500_000)));

        assertThat(outcome.verdict()).isEqualTo(RuleVerdict.FAIL);
        assertThat(outcome.reason()).isNotBlank();
    }

    private ScoringContext context(BigDecimal amount) {
        CreditEntity credit = CreditEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .requestedAmount(amount)
                .termMonths(12)
                .status(CreditStatus.PENDING)
                .build();
        PaymentHistoryResponse history = new PaymentHistoryResponse(0, 0, 0, 0, false, BigDecimal.ZERO);
        return new ScoringContext(credit, history);
    }
}
