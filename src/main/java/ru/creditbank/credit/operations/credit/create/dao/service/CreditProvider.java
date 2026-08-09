package ru.creditbank.credit.operations.credit.create.dao.service;

import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.credit.create.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.create.dao.repository.CreditRepository;

@Service
public class CreditProvider {

    private final CreditRepository creditRepository;

    public CreditProvider(CreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }

    public CreditEntity save(CreditEntity creditEntity) {
        return creditRepository.save(creditEntity);
    }
}