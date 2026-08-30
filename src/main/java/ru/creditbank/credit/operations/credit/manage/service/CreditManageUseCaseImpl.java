package ru.creditbank.credit.operations.credit.manage.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.creditbank.credit.operations.config.AuthenticatedUser;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.credit.manage.rest.dto.CreditApplicationDetails;
import ru.creditbank.credit.operations.credit.manage.rest.dto.CreditApplicationDetailsMapper;
import ru.creditbank.credit.operations.credit.manage.rest.dto.StatusUpdateRequest;
import ru.creditbank.credit.operations.exception.CreditNotFoundException;

import java.util.UUID;

@Service
public class CreditManageUseCaseImpl implements CreditManageUseCase {
    private final CreditProvider creditProvider;
    private final CreditDecisionService creditDecisionService;
    private final LoanApprovalSaga loanApprovalSaga;
    private final CreditApplicationDetailsMapper creditApplicationDetailsMapper;

    public CreditManageUseCaseImpl(CreditProvider creditProvider,
                                    CreditDecisionService creditDecisionService,
                                    LoanApprovalSaga loanApprovalSaga,
                                    CreditApplicationDetailsMapper creditApplicationDetailsMapper) {
        this.creditProvider = creditProvider;
        this.creditDecisionService = creditDecisionService;
        this.loanApprovalSaga = loanApprovalSaga;
        this.creditApplicationDetailsMapper = creditApplicationDetailsMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public CreditApplicationDetails getApplicationDetails(AuthenticatedUser requester, UUID id) {
        CreditEntity credit = findOrThrow(id);
        return creditApplicationDetailsMapper.toDetails(credit);
    }

    @Override
    public void updateStatus(UUID id, StatusUpdateRequest request) {
        CreditStatus newStatus = CreditStatus.valueOf(request.status().name());
        if (newStatus == CreditStatus.APPROVED) {
            loanApprovalSaga.approve(id, request.managerComment(), request.interestRate());
        } else {
            creditDecisionService.reject(id, request.managerComment());
        }
    }

    private CreditEntity findOrThrow(UUID id) {
        return creditProvider.findById(id).orElseThrow(() -> new CreditNotFoundException(id));
    }
}
