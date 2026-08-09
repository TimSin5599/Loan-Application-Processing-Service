package ru.creditbank.credit.operations.credit.create.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.creditbank.credit.operations.credit.create.dao.entity.CreditEntity;

import java.util.UUID;

public interface CreditRepository extends JpaRepository<CreditEntity, UUID> {
}