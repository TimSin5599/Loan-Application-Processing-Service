package ru.creditbank.credit.operations.credit.scoring.service.rules;

import org.springframework.stereotype.Component;
import ru.creditbank.credit.operations.credit.scoring.service.RuleOutcome;
import ru.creditbank.credit.operations.credit.scoring.service.ScoringContext;
import ru.creditbank.credit.operations.credit.scoring.service.ScoringRule;

@Component
public class TermWithinAutoApprovalRangeRule implements ScoringRule {
    public static final int MAX_AUTO_APPROVAL_TERM_MONTHS = 60;

    @Override
    public RuleOutcome evaluate(ScoringContext context) {
        Integer termMonths = context.credit().getTermMonths();
        if (termMonths > MAX_AUTO_APPROVAL_TERM_MONTHS) {
            return RuleOutcome.abstain(
                    "Срок кредита %d мес. превышает %d мес. — нужна ручная проверка"
                            .formatted(termMonths, MAX_AUTO_APPROVAL_TERM_MONTHS));
        }
        return RuleOutcome.pass();
    }
}
