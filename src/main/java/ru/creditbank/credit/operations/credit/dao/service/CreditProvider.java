package ru.creditbank.credit.operations.credit.dao.service;

import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.repository.CreditRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class CreditProvider {
    private final CreditRepository creditRepository;

    public CreditProvider(CreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }

    public CreditEntity save(CreditEntity creditEntity) {
        return creditRepository.save(creditEntity);
    }

    public Optional<CreditEntity> findById(UUID id) {
        return creditRepository.findById(id);
    }

    public Optional<CreditEntity> findByIdForUpdate(UUID id) {
        return creditRepository.findByIdForUpdate(id);
    }
}