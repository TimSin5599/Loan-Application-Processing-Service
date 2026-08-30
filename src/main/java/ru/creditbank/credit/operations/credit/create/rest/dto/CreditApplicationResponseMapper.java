package ru.creditbank.credit.operations.credit.create.rest.dto;

import org.springframework.stereotype.Component;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;

import java.time.ZoneOffset;

@Component
public class CreditApplicationResponseMapper {
    public CreditApplicationResponse toResponse(CreditEntity credit) {
        return new CreditApplicationResponse(
                credit.getId(),
                credit.getStatus().name(),
                credit.getCreationDate().atOffset(ZoneOffset.UTC)
        );
    }
}
