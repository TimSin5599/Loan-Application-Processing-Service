package ru.creditbank.credit.operations.credit.manage.service;

import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;

import java.util.UUID;

public class CreditAlreadyDecidedException extends RuntimeException {
    public CreditAlreadyDecidedException(UUID creditId, CreditStatus currentStatus) {
        super("Заявка " + creditId + " уже обработана, текущий статус: " + currentStatus);
    }
}
