package ru.creditbank.credit.operations.credit.manage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;

@Service
public class CreditNotificationService {
    private static final Logger log = LoggerFactory.getLogger(CreditNotificationService.class);

    private final JavaMailSender mailSender;

    public CreditNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void notifyStatusChange(CreditEntity credit) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(credit.getUserEmail());
        message.setSubject("Ваша кредитная заявка #%s".formatted(credit.getId()));
        message.setText("""
                Уважаемый %s, ваша заявка на кредит переведена %s.
                Комментарий менеджера: %s""".formatted(
                credit.getUserFullName(),
                clientStatus(credit.getStatus()),
                credit.getManagerComment() == null ? "" : credit.getManagerComment()
        ));

        try {
            mailSender.send(message);
            log.info("Уведомление по заявке creditId={} отправлено, статус={}", credit.getId(), credit.getStatus());
        } catch (MailException e) {
            log.error("Не удалось отправить уведомление по заявке creditId={}", credit.getId(), e);
            throw new NotificationDeliveryFailedException(credit.getId(), e);
        }
    }

    private String clientStatus(CreditStatus status) {
        return switch (status) {
            case APPROVED -> "одобрена";
            case REJECTED -> "отклонена";
            case PENDING, APPROVAL_IN_PROGRESS -> "на рассмотрении";
        };
    }
}
