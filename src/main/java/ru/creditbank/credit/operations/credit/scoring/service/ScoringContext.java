package ru.creditbank.credit.operations.credit.scoring.service;

import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.loan.dto.PaymentHistoryResponse;

public record ScoringContext(CreditEntity credit, PaymentHistoryResponse history) {
}
