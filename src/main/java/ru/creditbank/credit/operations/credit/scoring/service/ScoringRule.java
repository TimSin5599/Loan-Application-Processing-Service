package ru.creditbank.credit.operations.credit.scoring.service;

/**
 * Одно бизнес-правило автоматического скоринга заявки.
 * PASS/FAIL — правило уверенно "за" или "против". ABSTAIN — правилу не хватает
 * данных или уверенности для решения, заявка в этом случае уходит на ручную проверку,
 * если только другое правило не вернёт FAIL.
 */
public interface ScoringRule {

    RuleOutcome evaluate(ScoringContext context);
}
