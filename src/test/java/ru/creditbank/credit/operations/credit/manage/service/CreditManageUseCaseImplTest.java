package ru.creditbank.credit.operations.credit.manage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.creditbank.credit.operations.config.AuthenticatedUser;
import ru.creditbank.credit.operations.config.Roles;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.credit.manage.rest.dto.CreditApplicationDetails;
import ru.creditbank.credit.operations.credit.manage.rest.dto.CreditApplicationDetailsMapper;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditManageUseCaseImplTest {
    @Mock
    private CreditProvider creditProvider;

    @Mock
    private CreditDecisionService creditDecisionService;

    @Mock
    private LoanApprovalSaga loanApprovalSaga;

    private CreditManageUseCaseImpl creditManageUseCase;

    @BeforeEach
    void setUp() {
        creditManageUseCase = new CreditManageUseCaseImpl(
                creditProvider, creditDecisionService, loanApprovalSaga, new CreditApplicationDetailsMapper());
    }

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
    void updateStatus_approved_delegatesToLoanApprovalSaga() {
        UUID id = UUID.randomUUID();
        StatusUpdateRequest request = new StatusUpdateRequest(
                ManagerDecisionStatus.APPROVED, "Одобрено", BigDecimal.valueOf(15.5));

        creditManageUseCase.updateStatus(id, request);

        verify(loanApprovalSaga).approve(id, "Одобрено", BigDecimal.valueOf(15.5));
        verify(creditDecisionService, never()).reject(any(), any());
    }

    @Test
    void updateStatus_rejected_delegatesToCreditDecisionService() {
        UUID id = UUID.randomUUID();
        StatusUpdateRequest request = new StatusUpdateRequest(ManagerDecisionStatus.REJECTED, "Отказано", null);

        creditManageUseCase.updateStatus(id, request);

        verify(creditDecisionService).reject(id, "Отказано");
        verify(loanApprovalSaga, never()).approve(any(), any(), any());
    }

    @Test
    void updateStatus_missingCredit_throwsNotFound() {
        UUID id = UUID.randomUUID();
        doThrow(new CreditNotFoundException(id))
                .when(creditDecisionService).reject(eq(id), any());

        StatusUpdateRequest request = new StatusUpdateRequest(ManagerDecisionStatus.REJECTED, null, null);

        assertThatThrownBy(() -> creditManageUseCase.updateStatus(id, request))
                .isInstanceOf(CreditNotFoundException.class);
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
