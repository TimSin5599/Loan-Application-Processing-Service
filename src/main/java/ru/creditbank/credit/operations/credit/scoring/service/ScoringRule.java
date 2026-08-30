package ru.creditbank.credit.operations.credit.scoring.service;

public interface ScoringRule {
    RuleOutcome evaluate(ScoringContext context);
}
