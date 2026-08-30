package ru.creditbank.credit.operations.credit.manage.service;

import ru.creditbank.credit.operations.config.AuthenticatedUser;
import ru.creditbank.credit.operations.credit.manage.rest.dto.CreditApplicationDetails;
import ru.creditbank.credit.operations.credit.manage.rest.dto.StatusUpdateRequest;

import java.util.UUID;

public interface CreditManageUseCase {
    CreditApplicationDetails getApplicationDetails(AuthenticatedUser requester, UUID id);

    void updateStatus(UUID id, StatusUpdateRequest request);
}
