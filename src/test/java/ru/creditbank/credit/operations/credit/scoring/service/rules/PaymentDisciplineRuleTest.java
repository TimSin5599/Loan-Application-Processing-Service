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

class PaymentDisciplineRuleTest {

    private final PaymentDisciplineRule rule = new PaymentDisciplineRule();

    @Test
    void evaluate_noPaymentHistory_abstains() {
        PaymentHistoryResponse history = new PaymentHistoryResponse(0, 0, 0, 0, false, BigDecimal.ZERO);

        assertThat(rule.evaluate(context(history)).verdict()).isEqualTo(RuleVerdict.ABSTAIN);
    }

    @Test
    void evaluate_activeOverdue_fails() {
        PaymentHistoryResponse history = new PaymentHistoryResponse(1, 1, 5, 1, true, BigDecimal.valueOf(50_000));

        RuleOutcome outcome = rule.evaluate(context(history));

        assertThat(outcome.verdict()).isEqualTo(RuleVerdict.FAIL);
        assertThat(outcome.reason()).isNotBlank();
    }

    @Test
    void evaluate_goodPaymentDiscipline_passes() {
        PaymentHistoryResponse history = new PaymentHistoryResponse(1, 0, 20, 1, false, BigDecimal.ZERO);

        assertThat(rule.evaluate(context(history)).verdict()).isEqualTo(RuleVerdict.PASS);
    }

    @Test
    void evaluate_tooManyLatePayments_fails() {
        PaymentHistoryResponse history = new PaymentHistoryResponse(1, 0, 5, 5, false, BigDecimal.ZERO);

        RuleOutcome outcome = rule.evaluate(context(history));

        assertThat(outcome.verdict()).isEqualTo(RuleVerdict.FAIL);
        assertThat(outcome.reason()).isNotBlank();
    }

    private ScoringContext context(PaymentHistoryResponse history) {
        CreditEntity credit = CreditEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .requestedAmount(BigDecimal.valueOf(100_000))
                .termMonths(12)
                .status(CreditStatus.PENDING)
                .build();
        return new ScoringContext(credit, history);
    }
}
