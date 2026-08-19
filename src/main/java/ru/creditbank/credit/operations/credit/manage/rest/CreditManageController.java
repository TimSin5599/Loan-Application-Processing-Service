package ru.creditbank.credit.operations.credit.manage.rest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.creditbank.credit.operations.config.AuthenticatedUser;
import ru.creditbank.credit.operations.credit.manage.rest.dto.CreditApplicationDetails;
import ru.creditbank.credit.operations.credit.manage.rest.dto.StatusUpdateRequest;
import ru.creditbank.credit.operations.credit.manage.service.CreditManageUseCase;

import java.util.UUID;

@RestController
public class CreditManageController {

    private final CreditManageUseCase creditManageUseCase;

    public CreditManageController(CreditManageUseCase creditManageUseCase) {
        this.creditManageUseCase = creditManageUseCase;
    }

    @GetMapping("/credit-service/api/credit/{id}")
    public CreditApplicationDetails getApplication(@AuthenticationPrincipal AuthenticatedUser requester,
                                                     @PathVariable UUID id) {
        return creditManageUseCase.getApplicationDetails(requester, id);
    }

    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/credit-service/api/credit/{id}/status")
    public void updateStatus(@PathVariable UUID id, @Valid @RequestBody StatusUpdateRequest request) {
        creditManageUseCase.updateStatus(id, request);
    }
}
