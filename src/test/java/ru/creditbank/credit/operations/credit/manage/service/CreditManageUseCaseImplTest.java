package ru.creditbank.credit.operations.credit.manage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.creditbank.credit.operations.config.AuthenticatedUser;
import ru.creditbank.credit.operations.config.Roles;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.credit.manage.rest.dto.CreditApplicationDetails;
import ru.creditbank.credit.operations.credit.manage.rest.dto.ManagerDecisionStatus;
import ru.creditbank.credit.operations.credit.manage.rest.dto.StatusUpdateRequest;
import ru.creditbank.credit.operations.exception.CreditNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditManageUseCaseImplTest {

    @Mock
    private CreditProvider creditProvider;

    @Mock
    private CreditDecisionService creditDecisionService;

    @InjectMocks
    private CreditManageUseCaseImpl creditManageUseCase;

    @Test
    void getApplicationDetails_forManager_returnsDetails() {
        UUID ownerId = UUID.randomUUID();
        CreditEntity credit = credit(ownerId);
        when(creditProvider.findById(credit.getId())).thenReturn(Optional.of(credit));
        AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER);

        CreditApplicationDetails details = creditManageUseCase.getApplicationDetails(manager, credit.getId());

        assertThat(details.id()).isEqualTo(credit.getId());
        assertThat(details.userInfo().email()).isEqualTo(credit.getUserEmail());
        assertThat(details.loanDetails().requestedAmount()).isEqualByComparingTo(credit.getRequestedAmount());
        assertThat(details.status()).isEqualTo(credit.getStatus().name());
    }

    @Test
    void getApplicationDetails_forOwner_returnsDetails() {
        UUID ownerId = UUID.randomUUID();
        CreditEntity credit = credit(ownerId);
        when(creditProvider.findById(credit.getId())).thenReturn(Optional.of(credit));
        AuthenticatedUser owner = new AuthenticatedUser(ownerId, credit.getUserEmail(), null);

        CreditApplicationDetails details = creditManageUseCase.getApplicationDetails(owner, credit.getId());

        assertThat(details.id()).isEqualTo(credit.getId());
    }

    @Test
    void getApplicationDetails_forAnyAuthenticatedUser_returnsDetails() {
        // Доступ к заявке ограничивается на уровне apigateway, а не в этом use case.
        CreditEntity credit = credit(UUID.randomUUID());
        when(creditProvider.findById(credit.getId())).thenReturn(Optional.of(credit));
        AuthenticatedUser stranger = new AuthenticatedUser(UUID.randomUUID(), "stranger@example.com", null);

        CreditApplicationDetails details = creditManageUseCase.getApplicationDetails(stranger, credit.getId());

        assertThat(details.id()).isEqualTo(credit.getId());
    }

    @Test
    void getApplicationDetails_missingCredit_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(creditProvider.findById(id)).thenReturn(Optional.empty());
        AuthenticatedUser manager = new AuthenticatedUser(UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER);

        assertThatThrownBy(() -> creditManageUseCase.getApplicationDetails(manager, id))
                .isInstanceOf(CreditNotFoundException.class);
    }

    @Test
    void updateStatus_delegatesToCreditDecisionService() {
        CreditEntity credit = credit(UUID.randomUUID());
        when(creditProvider.findById(credit.getId())).thenReturn(Optional.of(credit));

        StatusUpdateRequest request = new StatusUpdateRequest(
                ManagerDecisionStatus.APPROVED, "Одобрено", BigDecimal.valueOf(15.5));

        creditManageUseCase.updateStatus(credit.getId(), request);

        ArgumentCaptor<CreditStatus> statusCaptor = ArgumentCaptor.forClass(CreditStatus.class);
        verify(creditDecisionService).applyDecision(
                eq(credit), statusCaptor.capture(), eq("Одобрено"), eq(BigDecimal.valueOf(15.5)));
        assertThat(statusCaptor.getValue()).isEqualTo(CreditStatus.APPROVED);
    }

    @Test
    void updateStatus_missingCredit_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(creditProvider.findById(id)).thenReturn(Optional.empty());

        StatusUpdateRequest request = new StatusUpdateRequest(ManagerDecisionStatus.APPROVED, null, null);

        assertThatThrownBy(() -> creditManageUseCase.updateStatus(id, request))
                .isInstanceOf(CreditNotFoundException.class);

        verify(creditDecisionService, never()).applyDecision(any(), any(), any(), any());
    }

    private CreditEntity credit(UUID ownerId) {
        LocalDateTime now = LocalDateTime.now();
        return CreditEntity.builder()
                .id(UUID.randomUUID())
                .userId(ownerId)
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
