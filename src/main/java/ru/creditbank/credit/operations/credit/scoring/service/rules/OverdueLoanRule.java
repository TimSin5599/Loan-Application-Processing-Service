package ru.creditbank.credit.operations.credit.scoring.service.rules;

import org.springframework.stereotype.Component;
import ru.creditbank.credit.operations.credit.scoring.service.RuleOutcome;
import ru.creditbank.credit.operations.credit.scoring.service.ScoringContext;
import ru.creditbank.credit.operations.credit.scoring.service.ScoringRule;
import ru.creditbank.credit.operations.loan.dto.PaymentHistoryResponse;

@Component
public class OverdueLoanRule implements ScoringRule {
    @Override
    public RuleOutcome evaluate(ScoringContext context) {
        PaymentHistoryResponse history = context.history();

        if (history.hasActiveOverdue()) {
            return RuleOutcome.fail("По текущим кредитам клиента есть активная просрочка");
        }
        return RuleOutcome.pass();
    }
}
