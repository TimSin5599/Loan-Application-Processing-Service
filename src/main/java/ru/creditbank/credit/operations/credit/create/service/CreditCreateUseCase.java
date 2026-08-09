package ru.creditbank.credit.operations.credit.create.service;

import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationRequest;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationResponse;

import java.util.UUID;

public interface CreditCreateUseCase {

    CreditApplicationResponse createApplication(UUID userId, CreditApplicationRequest request);
}