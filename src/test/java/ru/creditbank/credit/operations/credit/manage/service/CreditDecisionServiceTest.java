package ru.creditbank.credit.operations.credit.manage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.exception.CreditNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditDecisionServiceTest {
    @Mock
    private CreditProvider creditProvider;

    @Mock
    private CreditNotificationService creditNotificationService;

    @InjectMocks
    private CreditDecisionService creditDecisionService;

    @Test
    void reject_setsStatusSavesAndNotifies() {
        CreditEntity credit = credit(CreditStatus.PENDING);
        when(creditProvider.findByIdForUpdate(credit.getId())).thenReturn(Optional.of(credit));
        when(creditProvider.save(any(CreditEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreditEntity saved = creditDecisionService.reject(credit.getId(), "Отказано");

        assertThat(saved.getStatus()).isEqualTo(CreditStatus.REJECTED);
        assertThat(saved.getManagerComment()).isEqualTo("Отказано");
        verify(creditNotificationService).notifyStatusChange(saved);
    }

    @Test
    void reject_alreadyDecided_throwsWithoutSavingOrNotifying() {
        CreditEntity credit = credit(CreditStatus.APPROVED);
        when(creditProvider.findByIdForUpdate(credit.getId())).thenReturn(Optional.of(credit));

        assertThatThrownBy(() -> creditDecisionService.reject(credit.getId(), "Повторно"))
                .isInstanceOf(CreditAlreadyDecidedException.class);

        verify(creditProvider, never()).save(any());
        verify(creditNotificationService, never()).notifyStatusChange(any());
    }

    @Test
    void beginApproval_setsInProgressAndInterestRate_doesNotNotify() {
        CreditEntity credit = credit(CreditStatus.PENDING);
        when(creditProvider.findByIdForUpdate(credit.getId())).thenReturn(Optional.of(credit));
        when(creditProvider.save(any(CreditEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreditEntity saved = creditDecisionService.beginApproval(credit.getId(), "Одобрено", BigDecimal.valueOf(15.5));

        assertThat(saved.getStatus()).isEqualTo(CreditStatus.APPROVAL_IN_PROGRESS);
        assertThat(saved.getManagerComment()).isEqualTo("Одобрено");
        assertThat(saved.getInterestRate()).isEqualByComparingTo(BigDecimal.valueOf(15.5));
        verify(creditNotificationService, never()).notifyStatusChange(any());
    }

    @Test
    void beginApproval_alreadyDecided_throws() {
        CreditEntity credit = credit(CreditStatus.APPROVAL_IN_PROGRESS);
        when(creditProvider.findByIdForUpdate(credit.getId())).thenReturn(Optional.of(credit));

        assertThatThrownBy(() -> creditDecisionService.beginApproval(credit.getId(), "Одобрено", null))
                .isInstanceOf(CreditAlreadyDecidedException.class);

        verify(creditProvider, never()).save(any());
    }

    @Test
    void completeApproval_setsApprovedAndNotifies() {
        CreditEntity credit = credit(CreditStatus.APPROVAL_IN_PROGRESS);
        when(creditProvider.findByIdForUpdate(credit.getId())).thenReturn(Optional.of(credit));
        when(creditProvider.save(any(CreditEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreditEntity saved = creditDecisionService.completeApproval(credit.getId());

        assertThat(saved.getStatus()).isEqualTo(CreditStatus.APPROVED);
        verify(creditNotificationService).notifyStatusChange(saved);
    }

    @Test
    void revertFailedApproval_setsPendingWithReason_doesNotNotify() {
        CreditEntity credit = credit(CreditStatus.APPROVAL_IN_PROGRESS);
        when(creditProvider.findByIdForUpdate(credit.getId())).thenReturn(Optional.of(credit));
        when(creditProvider.save(any(CreditEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreditEntity saved = creditDecisionService.revertFailedApproval(credit.getId(), "Не удалось создать кредит");

        assertThat(saved.getStatus()).isEqualTo(CreditStatus.PENDING);
        assertThat(saved.getManagerComment()).isEqualTo("Не удалось создать кредит");
        verify(creditNotificationService, never()).notifyStatusChange(any());
    }

    @Test
    void reject_missingCredit_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(creditProvider.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> creditDecisionService.reject(id, null))
                .isInstanceOf(CreditNotFoundException.class);
    }

    private CreditEntity credit(CreditStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return CreditEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .userEmail("ivanov@example.com")
                .userFullName("Иванов Иван Иванович")
                .requestedAmount(BigDecimal.valueOf(50000))
                .termMonths(12)
                .status(status)
                .creationDate(now)
                .lastUpdated(now)
                .build();
    }
}
