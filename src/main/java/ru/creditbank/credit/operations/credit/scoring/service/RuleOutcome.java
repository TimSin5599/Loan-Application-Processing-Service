package ru.creditbank.credit.operations.credit.scoring.service;

public record RuleOutcome(RuleVerdict verdict, String reason) {
    public static RuleOutcome pass() {
        return new RuleOutcome(RuleVerdict.PASS, null);
    }

    public static RuleOutcome fail(String reason) {
        return new RuleOutcome(RuleVerdict.FAIL, reason);
    }

    public static RuleOutcome abstain(String reason) {
        return new RuleOutcome(RuleVerdict.ABSTAIN, reason);
    }
}
