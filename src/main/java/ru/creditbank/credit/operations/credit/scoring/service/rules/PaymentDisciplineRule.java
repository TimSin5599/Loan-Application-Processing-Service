package ru.creditbank.credit.operations.credit.scoring.service.rules;

import org.springframework.stereotype.Component;
import ru.creditbank.credit.operations.credit.scoring.service.RuleOutcome;
import ru.creditbank.credit.operations.credit.scoring.service.ScoringContext;
import ru.creditbank.credit.operations.credit.scoring.service.ScoringRule;
import ru.creditbank.credit.operations.loan.dto.PaymentHistoryResponse;

/**
 * Учитывает добросовестность платежей клиента: активная просрочка — сразу отказ,
 * высокая доля просроченных платежей в истории — тоже отказ. Без истории платежей
 * оценивать нечего — правило воздерживается.
 */
@Component
public class PaymentDisciplineRule implements ScoringRule {

    public static final double MAX_LATE_PAYMENT_RATIO = 0.2;

    @Override
    public RuleOutcome evaluate(ScoringContext context) {
        PaymentHistoryResponse history = context.history();

        if (history.hasActiveOverdue()) {
            return RuleOutcome.fail("По текущим кредитам клиента есть активная просрочка");
        }

        int totalPayments = history.onTimePayments() + history.latePayments();
        if (totalPayments == 0) {
            return RuleOutcome.abstain("Нет истории платежей для оценки дисциплины");
        }

        double lateRatio = (double) history.latePayments() / totalPayments;
        if (lateRatio > MAX_LATE_PAYMENT_RATIO) {
            return RuleOutcome.fail(
                    "Доля просроченных платежей %.0f%% выше допустимой %.0f%%"
                            .formatted(lateRatio * 100, MAX_LATE_PAYMENT_RATIO * 100));
        }
        return RuleOutcome.pass();
    }
}
