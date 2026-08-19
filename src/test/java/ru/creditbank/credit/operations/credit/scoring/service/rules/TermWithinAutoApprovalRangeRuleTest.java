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

class TermWithinAutoApprovalRangeRuleTest {

    private final TermWithinAutoApprovalRangeRule rule = new TermWithinAutoApprovalRangeRule();

    @Test
    void evaluate_termWithinRange_passes() {
        assertThat(rule.evaluate(context(36)).verdict()).isEqualTo(RuleVerdict.PASS);
    }

    @Test
    void evaluate_termTooLong_abstains() {
        RuleOutcome outcome = rule.evaluate(context(72));

        assertThat(outcome.verdict()).isEqualTo(RuleVerdict.ABSTAIN);
        assertThat(outcome.reason()).isNotBlank();
    }

    private ScoringContext context(int termMonths) {
        CreditEntity credit = CreditEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .requestedAmount(BigDecimal.valueOf(100_000))
                .termMonths(termMonths)
                .status(CreditStatus.PENDING)
                .build();
        PaymentHistoryResponse history = new PaymentHistoryResponse(0, 0, 0, 0, false, BigDecimal.ZERO);
        return new ScoringContext(credit, history);
    }
}
