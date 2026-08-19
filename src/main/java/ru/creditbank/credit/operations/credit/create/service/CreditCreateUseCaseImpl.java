package ru.creditbank.credit.operations.credit.create.service;

import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.config.AuthenticatedUser;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationRequest;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationResponse;
import ru.creditbank.credit.operations.credit.scoring.service.CreditScoringService;

import java.time.ZoneOffset;

@Service
public class CreditCreateUseCaseImpl implements CreditCreateUseCase {

    private final CreditProvider creditProvider;
    private final CreditScoringService creditScoringService;

    public CreditCreateUseCaseImpl(CreditProvider creditProvider, CreditScoringService creditScoringService) {
        this.creditProvider = creditProvider;
        this.creditScoringService = creditScoringService;
    }

    @Override
    public CreditApplicationResponse createApplication(AuthenticatedUser applicant, CreditApplicationRequest request) {
        CreditEntity creditEntity = CreditEntity.builder()
                .userId(applicant.userId())
                .userEmail(applicant.email())
                .userFullName(request.fullName())
                .requestedAmount(request.requestedAmount())
                .termMonths(request.termMonths())
                .status(CreditStatus.PENDING)
                .build();

        CreditEntity saved = creditProvider.save(creditEntity);
        creditScoringService.scoreApplication(saved.getId());

        return new CreditApplicationResponse(
                saved.getId(),
                saved.getStatus().name(),
                saved.getCreationDate().atOffset(ZoneOffset.UTC)
        );
    }
}