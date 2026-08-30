package ru.creditbank.credit.operations.credit.manage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.service.CreditProvider;
import ru.creditbank.credit.operations.exception.CreditNotFoundException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CreditDecisionService {
    private static final Logger log = LoggerFactory.getLogger(CreditDecisionService.class);

    private final CreditProvider creditProvider;
    private final CreditNotificationService creditNotificationService;

    public CreditDecisionService(CreditProvider creditProvider,
                                  CreditNotificationService creditNotificationService) {
        this.creditProvider = creditProvider;
        this.creditNotificationService = creditNotificationService;
    }

    @Transactional(noRollbackFor = NotificationDeliveryFailedException.class)
    public CreditEntity reject(UUID creditId, String comment) {
        CreditEntity credit = findPendingForUpdate(creditId);
        credit.setStatus(CreditStatus.REJECTED);
        credit.setManagerComment(comment);

        CreditEntity saved = creditProvider.save(credit);
        log.info("Заявка отклонена creditId={}", creditId);
        creditNotificationService.notifyStatusChange(saved);
        return saved;
    }

    @Transactional
    public CreditEntity beginApproval(UUID creditId, String comment, BigDecimal interestRate) {
        CreditEntity credit = findPendingForUpdate(creditId);
        if (interestRate != null) {
            credit.setInterestRate(interestRate);
        }
        credit.setStatus(CreditStatus.APPROVAL_IN_PROGRESS);
        credit.setManagerComment(comment);

        CreditEntity saved = creditProvider.save(credit);
        log.info("Начато одобрение заявки creditId={}, выполняется выдача кредита в loan-management-service", creditId);
        return saved;
    }

    @Transactional(noRollbackFor = NotificationDeliveryFailedException.class)
    public CreditEntity completeApproval(UUID creditId) {
        CreditEntity credit = creditProvider.findByIdForUpdate(creditId)
                .orElseThrow(() -> new CreditNotFoundException(creditId));
        credit.setStatus(CreditStatus.APPROVED);

        CreditEntity saved = creditProvider.save(credit);
        log.info("Заявка одобрена creditId={}", creditId);
        creditNotificationService.notifyStatusChange(saved);
        return saved;
    }

    @Transactional
    public CreditEntity revertFailedApproval(UUID creditId, String failureReason) {
        CreditEntity credit = creditProvider.findByIdForUpdate(creditId)
                .orElseThrow(() -> new CreditNotFoundException(creditId));
        credit.setStatus(CreditStatus.PENDING);
        credit.setManagerComment(failureReason);

        CreditEntity saved = creditProvider.save(credit);
        log.warn("Одобрение заявки creditId={} не завершено, статус возвращён в PENDING: {}", creditId, failureReason);
        return saved;
    }

    private CreditEntity findPendingForUpdate(UUID creditId) {
        CreditEntity credit = creditProvider.findByIdForUpdate(creditId)
                .orElseThrow(() -> new CreditNotFoundException(creditId));

        if (credit.getStatus() != CreditStatus.PENDING) {
            log.warn("Попытка повторно решить уже обработанную заявку creditId={} currentStatus={}",
                    creditId, credit.getStatus());
            throw new CreditAlreadyDecidedException(creditId, credit.getStatus());
        }
        return credit;
    }
}
