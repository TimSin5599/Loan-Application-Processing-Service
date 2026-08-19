package ru.creditbank.credit.operations.credit.manage.service;

import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.config.AuthenticatedUser;
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
    private final CreditDecisionService creditDecisionService;

    public CreditManageUseCaseImpl(CreditProvider creditProvider,
                                    CreditDecisionService creditDecisionService) {
        this.creditProvider = creditProvider;
        this.creditDecisionService = creditDecisionService;
    }

    @Override
    public CreditApplicationDetails getApplicationDetails(AuthenticatedUser requester, UUID id) {
        CreditEntity credit = findOrThrow(id);

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
        CreditStatus newStatus = CreditStatus.valueOf(request.status().name());
        creditDecisionService.applyDecision(credit, newStatus, request.managerComment(), request.interestRate());
    }

    private CreditEntity findOrThrow(UUID id) {
        return creditProvider.findById(id).orElseThrow(() -> new CreditNotFoundException(id));
    }
}
