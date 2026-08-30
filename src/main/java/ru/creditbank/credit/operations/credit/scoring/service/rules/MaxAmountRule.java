package ru.creditbank.credit.operations.credit.scoring.service.rules;

import org.springframework.stereotype.Component;
import ru.creditbank.credit.operations.credit.scoring.service.RuleOutcome;
import ru.creditbank.credit.operations.credit.scoring.service.ScoringContext;
import ru.creditbank.credit.operations.credit.scoring.service.ScoringRule;

import java.math.BigDecimal;

@Component
public class MaxAmountRule implements ScoringRule {
    public static final BigDecimal MAX_AUTO_APPROVAL_AMOUNT = BigDecimal.valueOf(3_000_000);

    @Override
    public RuleOutcome evaluate(ScoringContext context) {
        BigDecimal amount = context.credit().getRequestedAmount();
        if (amount.compareTo(MAX_AUTO_APPROVAL_AMOUNT) > 0) {
            return RuleOutcome.fail(
                    "Сумма заявки %s превышает лимит автоматического одобрения %s"
                            .formatted(amount, MAX_AUTO_APPROVAL_AMOUNT));
        }
        return RuleOutcome.pass();
    }
}
