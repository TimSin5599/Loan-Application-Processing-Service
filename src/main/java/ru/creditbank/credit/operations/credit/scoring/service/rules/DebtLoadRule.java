package ru.creditbank.credit.operations.credit.scoring.service.rules;

import org.springframework.stereotype.Component;
import ru.creditbank.credit.operations.credit.scoring.service.RuleOutcome;
import ru.creditbank.credit.operations.credit.scoring.service.ScoringContext;
import ru.creditbank.credit.operations.credit.scoring.service.ScoringRule;
import ru.creditbank.credit.operations.loan.dto.PaymentHistoryResponse;

import java.math.BigDecimal;

@Component
public class DebtLoadRule implements ScoringRule {
    public static final BigDecimal MAX_TOTAL_DEBT = BigDecimal.valueOf(5_000_000);

    @Override
    public RuleOutcome evaluate(ScoringContext context) {
        PaymentHistoryResponse history = context.history();
        if (history.totalLoans() == 0) {
            return RuleOutcome.abstain("Нет кредитной истории для оценки долговой нагрузки");
        }

        BigDecimal projectedDebt = history.totalOutstandingDebt().add(context.credit().getRequestedAmount());
        if (projectedDebt.compareTo(MAX_TOTAL_DEBT) > 0) {
            return RuleOutcome.fail(
                    "Суммарная долговая нагрузка %s превысит допустимый порог %s"
                            .formatted(projectedDebt, MAX_TOTAL_DEBT));
        }
        return RuleOutcome.pass();
    }
}
