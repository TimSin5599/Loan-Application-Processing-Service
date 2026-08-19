package ru.creditbank.credit.operations.credit.scoring.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.credit.manage.service.CreditDecisionService;
import ru.creditbank.credit.operations.exception.CreditNotFoundException;
import ru.creditbank.credit.operations.loan.PaymentHistoryClient;
import ru.creditbank.credit.operations.loan.PaymentHistoryUnavailableException;
import ru.creditbank.credit.operations.loan.dto.PaymentHistoryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditScoringServiceTest {

    @Mock
    private CreditProvider creditProvider;

    @Mock
    private PaymentHistoryClient paymentHistoryClient;

    @Mock
    private CreditDecisionService creditDecisionService;

    private static final PaymentHistoryResponse EMPTY_HISTORY =
            new PaymentHistoryResponse(0, 0, 0, 0, false, BigDecimal.ZERO);

    @Test
    void scoreApplication_allRulesPass_approvesAutomatically() {
        CreditEntity credit = credit();
        when(creditProvider.findById(credit.getId())).thenReturn(Optional.of(credit));
        when(paymentHistoryClient.fetchHistory(credit.getUserId())).thenReturn(EMPTY_HISTORY);
        CreditScoringService service = serviceWithRules(alwaysReturning(RuleOutcome.pass()));

        service.scoreApplication(credit.getId());

        ArgumentCaptor<BigDecimal> rateCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(creditDecisionService).applyDecision(
                eq(credit),
                eq(CreditStatus.APPROVED),
                any(),
                rateCaptor.capture());
        assertThat(rateCaptor.getValue()).isNotNull();
    }

    @Test
    void scoreApplication_oneRuleFails_rejectsAutomaticallyWithReason() {
        CreditEntity credit = credit();
        when(creditProvider.findById(credit.getId())).thenReturn(Optional.of(credit));
        when(paymentHistoryClient.fetchHistory(credit.getUserId())).thenReturn(EMPTY_HISTORY);
        CreditScoringService service = serviceWithRules(
                alwaysReturning(RuleOutcome.pass()),
                alwaysReturning(RuleOutcome.fail("Слишком большая сумма")));

        service.scoreApplication(credit.getId());

        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(creditDecisionService).applyDecision(
                eq(credit),
                eq(CreditStatus.REJECTED),
                reasonCaptor.capture(),
                isNull());
        assertThat(reasonCaptor.getValue()).contains("Слишком большая сумма");
    }

    @Test
    void scoreApplication_ruleAbstainsWithoutFailures_leavesPendingForManualReview() {
        CreditEntity credit = credit();
        when(creditProvider.findById(credit.getId())).thenReturn(Optional.of(credit));
        when(paymentHistoryClient.fetchHistory(credit.getUserId())).thenReturn(EMPTY_HISTORY);
        CreditScoringService service = serviceWithRules(
                alwaysReturning(RuleOutcome.pass()),
                alwaysReturning(RuleOutcome.abstain("Нужна ручная проверка")));

        service.scoreApplication(credit.getId());

        verify(creditDecisionService, never()).applyDecision(any(), any(), any(), any());
    }

    @Test
    void scoreApplication_paymentHistoryUnavailable_leavesPendingForManualReview() {
        CreditEntity credit = credit();
        when(creditProvider.findById(credit.getId())).thenReturn(Optional.of(credit));
        when(paymentHistoryClient.fetchHistory(credit.getUserId()))
                .thenThrow(new PaymentHistoryUnavailableException(credit.getUserId(), new RuntimeException("timeout")));
        CreditScoringService service = serviceWithRules(alwaysReturning(RuleOutcome.pass()));

        service.scoreApplication(credit.getId());

        verify(creditDecisionService, never()).applyDecision(any(), any(), any(), any());
    }

    @Test
    void scoreApplication_creditAlreadyDecided_doesNothing() {
        CreditEntity credit = credit();
        credit.setStatus(CreditStatus.APPROVED);
        when(creditProvider.findById(credit.getId())).thenReturn(Optional.of(credit));
        CreditScoringService service = serviceWithRules(alwaysReturning(RuleOutcome.pass()));

        service.scoreApplication(credit.getId());

        verify(paymentHistoryClient, never()).fetchHistory(any());
        verify(creditDecisionService, never()).applyDecision(any(), any(), any(), any());
    }

    @Test
    void scoreApplication_missingCredit_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(creditProvider.findById(id)).thenReturn(Optional.empty());
        CreditScoringService service = serviceWithRules(alwaysReturning(RuleOutcome.pass()));

        assertThatThrownBy(() -> service.scoreApplication(id)).isInstanceOf(CreditNotFoundException.class);
    }

    private CreditScoringService serviceWithRules(ScoringRule... rules) {
        return new CreditScoringService(creditProvider, paymentHistoryClient, creditDecisionService, List.of(rules));
    }

    private ScoringRule alwaysReturning(RuleOutcome outcome) {
        return context -> outcome;
    }

    private CreditEntity credit() {
        LocalDateTime now = LocalDateTime.now();
        return CreditEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .userEmail("ivanov@example.com")
                .userFullName("Иванов Иван Иванович")
                .requestedAmount(BigDecimal.valueOf(100_000))
                .termMonths(12)
                .status(CreditStatus.PENDING)
                .creationDate(now)
                .lastUpdated(now)
                .build();
    }
}
