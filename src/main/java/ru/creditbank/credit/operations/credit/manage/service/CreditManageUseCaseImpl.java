package ru.creditbank.credit.operations.credit.manage.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.config.AuthenticatedUser;
import ru.creditbank.credit.operations.config.Roles;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.credit.manage.rest.dto.CreditApplicationDetails;
import ru.creditbank.credit.operations.credit.manage.rest.dto.StatusUpdateRequest;
import ru.creditbank.credit.operations.exception.CreditNotFoundException;

import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class CreditManageUseCaseImpl implements CreditManageUseCase {

    private final CreditProvider creditProvider;
    private final CreditNotificationService creditNotificationService;

    public CreditManageUseCaseImpl(CreditProvider creditProvider, CreditNotificationService creditNotificationService) {
        this.creditProvider = creditProvider;
        this.creditNotificationService = creditNotificationService;
    }

    @Override
    public CreditApplicationDetails getApplicationDetails(AuthenticatedUser requester, UUID id) {
        CreditEntity credit = findOrThrow(id);

        boolean isManager = Roles.CREDIT_MANAGER.equals(requester.role());
        boolean isOwner = credit.getUserId().equals(requester.userId());
        if (!isManager && !isOwner) {
            throw new AccessDeniedException("Недостаточно прав для просмотра заявки " + id);
        }

        return new CreditApplicationDetails(
                credit.getId(),
                new CreditApplicationDetails.UserInfo(
                        credit.getUserId().toString(),
                        credit.getUserFullName(),
                        credit.getUserEmail()
                ),
                new CreditApplicationDetails.LoanDetails(
                        credit.getRequestedAmount(),
                        credit.getTermMonths(),
                        credit.getInterestRate()
                ),
                credit.getStatus().name(),
                credit.getCreationDate().atOffset(ZoneOffset.UTC)
        );
    }

    @Override
    public void updateStatus(UUID id, StatusUpdateRequest request) {
        CreditEntity credit = findOrThrow(id);

        credit.setStatus(CreditStatus.valueOf(request.status().name()));
        credit.setManagerComment(request.managerComment());
        if (request.interestRate() != null) {
            credit.setInterestRate(request.interestRate());
        }

        CreditEntity saved = creditProvider.save(credit);
        creditNotificationService.notifyStatusChange(saved);
    }

    private CreditEntity findOrThrow(UUID id) {
        return creditProvider.findById(id).orElseThrow(() -> new CreditNotFoundException(id));
    }
}
