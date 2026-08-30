package ru.creditbank.credit.operations.credit.create.rest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.creditbank.credit.operations.config.AuthenticatedUser;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationRequest;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationResponse;
import ru.creditbank.credit.operations.credit.create.service.CreditCreateUseCase;

@RestController
public class CreditApplicationController {
    private final CreditCreateUseCase creditCreateUseCase;

    public CreditApplicationController(CreditCreateUseCase creditCreateUseCase) {
        this.creditCreateUseCase = creditCreateUseCase;
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/credit-service/api/v1/credit/")
    public CreditApplicationResponse createApplication(@AuthenticationPrincipal AuthenticatedUser applicant,
                                                         @Valid @RequestBody CreditApplicationRequest request) {
        return creditCreateUseCase.createApplication(applicant, request);
    }
}