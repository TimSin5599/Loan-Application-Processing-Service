package ru.creditbank.credit.operations.credit.manage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.loan.LoanIssuanceClient;
import ru.creditbank.credit.operations.loan.LoanIssuanceFailedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApprovalSagaTest {
    @Mock
    private CreditDecisionService creditDecisionService;

    @Mock
    private LoanIssuanceClient loanIssuanceClient;

    @InjectMocks
    private LoanApprovalSaga loanApprovalSaga;

    @Test
    void approve_allStepsSucceed_completesApproval() {
        UUID creditId = UUID.randomUUID();
        UUID loanId = UUID.randomUUID();
        CreditEntity credit = credit(creditId);
        when(creditDecisionService.beginApproval(creditId, "Одобрено", BigDecimal.valueOf(15.5))).thenReturn(credit);
        when(loanIssuanceClient.issueLoan(credit)).thenReturn(loanId);

        loanApprovalSaga.approve(creditId, "Одобрено", BigDecimal.valueOf(15.5));

        InOrder order = inOrder(loanIssuanceClient, creditDecisionService);
        order.verify(creditDecisionService).beginApproval(creditId, "Одобрено", BigDecimal.valueOf(15.5));
        order.verify(loanIssuanceClient).issueLoan(credit);
        order.verify(loanIssuanceClient).createPaymentSchedule(creditId, loanId);
        order.verify(creditDecisionService).completeApproval(creditId);
        verify(loanIssuanceClient, never()).cancelLoan(any(), any());
        verify(creditDecisionService, never()).revertFailedApproval(any(), any());
    }

    @Test
    void approve_issueLoanFails_revertsToPendingWithoutSchedulingOrCancelling() {
        UUID creditId = UUID.randomUUID();
        CreditEntity credit = credit(creditId);
        when(creditDecisionService.beginApproval(any(), any(), any())).thenReturn(credit);
        doThrow(new RuntimeException("connection refused")).when(loanIssuanceClient).issueLoan(credit);

        assertThatThrownBy(() -> loanApprovalSaga.approve(creditId, "Одобрено", null))
                .isInstanceOf(LoanIssuanceFailedException.class);

        verify(loanIssuanceClient, never()).createPaymentSchedule(any(), any());
        verify(loanIssuanceClient, never()).cancelLoan(any(), any());
        verify(creditDecisionService).revertFailedApproval(eq(creditId), any());
        verify(creditDecisionService, never()).completeApproval(any());
    }

    @Test
    void approve_scheduleFails_compensatesByCancellingLoanAndRevertsToPending() {
        UUID creditId = UUID.randomUUID();
        UUID loanId = UUID.randomUUID();
        CreditEntity credit = credit(creditId);
        when(creditDecisionService.beginApproval(any(), any(), any())).thenReturn(credit);
        when(loanIssuanceClient.issueLoan(credit)).thenReturn(loanId);
        doThrow(new RuntimeException("timeout")).when(loanIssuanceClient).createPaymentSchedule(creditId, loanId);

        assertThatThrownBy(() -> loanApprovalSaga.approve(creditId, "Одобрено", null))
                .isInstanceOf(LoanIssuanceFailedException.class);

        verify(loanIssuanceClient).cancelLoan(creditId, loanId);
        verify(creditDecisionService).revertFailedApproval(eq(creditId), any());
        verify(creditDecisionService, never()).completeApproval(any());
    }

    @Test
    void approve_scheduleAndCompensationBothFail_stillRevertsToPendingAndThrows() {
        UUID creditId = UUID.randomUUID();
        UUID loanId = UUID.randomUUID();
        CreditEntity credit = credit(creditId);
        when(creditDecisionService.beginApproval(any(), any(), any())).thenReturn(credit);
        when(loanIssuanceClient.issueLoan(credit)).thenReturn(loanId);
        doThrow(new RuntimeException("timeout")).when(loanIssuanceClient).createPaymentSchedule(creditId, loanId);
        doThrow(new RuntimeException("loan-management-service unreachable"))
                .when(loanIssuanceClient).cancelLoan(creditId, loanId);

        assertThatThrownBy(() -> loanApprovalSaga.approve(creditId, "Одобрено", null))
                .isInstanceOf(LoanIssuanceFailedException.class);

        verify(creditDecisionService).revertFailedApproval(eq(creditId), any());
    }

    private CreditEntity credit(UUID creditId) {
        LocalDateTime now = LocalDateTime.now();
        return CreditEntity.builder()
                .id(creditId)
                .userId(UUID.randomUUID())
                .userEmail("ivanov@example.com")
                .userFullName("Иванов Иван Иванович")
                .requestedAmount(BigDecimal.valueOf(50000))
                .termMonths(12)
                .status(CreditStatus.APPROVAL_IN_PROGRESS)
                .creationDate(now)
                .lastUpdated(now)
                .build();
    }
}
