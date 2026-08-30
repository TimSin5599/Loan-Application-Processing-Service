package ru.creditbank.credit.operations.credit.create.service;

import ru.creditbank.credit.operations.config.AuthenticatedUser;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationRequest;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationResponse;

public interface CreditCreateUseCase {
    CreditApplicationResponse createApplication(AuthenticatedUser applicant, CreditApplicationRequest request);
}