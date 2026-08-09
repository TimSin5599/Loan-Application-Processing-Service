package ru.creditbank.credit.operations.credit.create.service;

import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.credit.create.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.create.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.create.dao.service.CreditProvider;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationRequest;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationResponse;

import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class CreditCreateUseCaseImpl implements CreditCreateUseCase {

    private final CreditProvider creditProvider;

    public CreditCreateUseCaseImpl(CreditProvider creditProvider) {
        this.creditProvider = creditProvider;
    }

    @Override
    public CreditApplicationResponse createApplication(UUID userId, CreditApplicationRequest request) {
        CreditEntity creditEntity = CreditEntity.builder()
                .userId(userId)
                .userFullName(request.fullName())
                .requestedAmount(request.requestedAmount())
                .termMonths(request.termMonths())
                .status(CreditStatus.PENDING)
                .build();

        CreditEntity saved = creditProvider.save(creditEntity);

        return new CreditApplicationResponse(
                saved.getId(),
                saved.getStatus().name(),
                saved.getCreationDate().atOffset(ZoneOffset.UTC)
        );
    }
}