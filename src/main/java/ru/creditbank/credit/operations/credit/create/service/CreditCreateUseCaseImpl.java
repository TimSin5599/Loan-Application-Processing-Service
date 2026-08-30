package ru.creditbank.credit.operations.credit.create.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.config.AuthenticatedUser;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationRequest;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationResponse;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationResponseMapper;
import ru.creditbank.credit.operations.credit.scoring.service.CreditScoringService;

@Service
public class CreditCreateUseCaseImpl implements CreditCreateUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreditCreateUseCaseImpl.class);

    private final CreditProvider creditProvider;
    private final CreditScoringService creditScoringService;
    private final CreditApplicationResponseMapper creditApplicationResponseMapper;

    public CreditCreateUseCaseImpl(CreditProvider creditProvider,
                                    CreditScoringService creditScoringService,
                                    CreditApplicationResponseMapper creditApplicationResponseMapper) {
        this.creditProvider = creditProvider;
        this.creditScoringService = creditScoringService;
        this.creditApplicationResponseMapper = creditApplicationResponseMapper;
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
        log.info("Создана заявка на кредит creditId={} amount={} termMonths={}",
                saved.getId(), saved.getRequestedAmount(), saved.getTermMonths());
        creditScoringService.scoreApplication(saved.getId());

        return creditApplicationResponseMapper.toResponse(saved);
    }
}