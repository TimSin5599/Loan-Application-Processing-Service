package ru.creditbank.credit.operations.credit.manage.rest.dto;

import org.springframework.stereotype.Component;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;

import java.time.ZoneOffset;

@Component
public class CreditApplicationDetailsMapper {
    public CreditApplicationDetails toDetails(CreditEntity credit) {
        return new CreditApplicationDetails(
                credit.getId(),
                new CreditApplicationDetails.UserInfo(
                        credit.getUserId().toString(),
                        credit.getUserFullName(),
                        credit.getUserEmail()
                ),
                new CreditApplicationDetails.LoanDetails(
                        credit.getRequestedAmount(),
                        credit.getTermMonths(),
                        credit.getInterestRate()
                ),
                credit.getStatus().name(),
                credit.getCreationDate().atOffset(ZoneOffset.UTC)
        );
    }
}
