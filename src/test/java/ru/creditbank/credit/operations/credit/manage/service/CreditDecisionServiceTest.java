package ru.creditbank.credit.operations.credit.manage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.loan.LoanIssuanceClient;
import ru.creditbank.credit.operations.loan.LoanIssuanceFailedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditDecisionServiceTest {

    @Mock
    private CreditProvider creditProvider;

    @Mock
    private CreditNotificationService creditNotificationService;

    @Mock
    private LoanIssuanceClient loanIssuanceClient;

    @InjectMocks
    private CreditDecisionService creditDecisionService;

    @Test
    void applyDecision_approved_setsInterestRateBeforeIssuingLoanSavesAndNotifies() {
        CreditEntity credit = credit();
        when(creditProvider.save(any(CreditEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreditEntity saved = creditDecisionService.applyDecision(
                credit, CreditStatus.APPROVED, "Одобрено", BigDecimal.valueOf(15.5));

        assertThat(saved.getStatus()).isEqualTo(CreditStatus.APPROVED);
        assertThat(saved.getManagerComment()).isEqualTo("Одобрено");
        assertThat(saved.getInterestRate()).isEqualByComparingTo(BigDecimal.valueOf(15.5));

        ArgumentCaptor<CreditEntity> issuedLoanFor = ArgumentCaptor.forClass(CreditEntity.class);
        verify(loanIssuanceClient).issueLoan(issuedLoanFor.capture());
        assertThat(issuedLoanFor.getValue().getInterestRate()).isEqualByComparingTo(BigDecimal.valueOf(15.5));

        verify(creditProvider).save(credit);
        verify(creditNotificationService).notifyStatusChange(saved);
    }

    @Test
    void applyDecision_rejected_doesNotIssueLoan() {
        CreditEntity credit = credit();
        when(creditProvider.save(any(CreditEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        creditDecisionService.applyDecision(credit, CreditStatus.REJECTED, "Отказано", null);

        verify(loanIssuanceClient, never()).issueLoan(any());
        verify(creditProvider).save(credit);
        verify(creditNotificationService).notifyStatusChange(credit);
    }

    @Test
    void applyDecision_loanIssuanceFails_doesNotSaveOrNotify() {
        CreditEntity credit = credit();
        doThrow(new LoanIssuanceFailedException(credit.getId(), new RuntimeException("connection refused")))
                .when(loanIssuanceClient).issueLoan(credit);

        assertThatThrownBy(() -> creditDecisionService.applyDecision(credit, CreditStatus.APPROVED, "Одобрено", null))
                .isInstanceOf(LoanIssuanceFailedException.class);

        assertThat(credit.getStatus()).isEqualTo(CreditStatus.PENDING);
        verify(creditProvider, never()).save(any());
        verify(creditNotificationService, never()).notifyStatusChange(any());
    }

    private CreditEntity credit() {
        LocalDateTime now = LocalDateTime.now();
        return CreditEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .userEmail("ivanov@example.com")
                .userFullName("Иванов Иван Иванович")
                .requestedAmount(BigDecimal.valueOf(50000))
                .termMonths(12)
                .status(CreditStatus.PENDING)
                .creationDate(now)
                .lastUpdated(now)
                .build();
    }
}
