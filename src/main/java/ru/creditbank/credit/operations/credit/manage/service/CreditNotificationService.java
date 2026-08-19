package ru.creditbank.credit.operations.credit.manage.service;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;

@Service
public class CreditNotificationService {

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
        } catch (MailException e) {
            throw new NotificationDeliveryFailedException(credit.getId(), e);
        }
    }

    private String clientStatus(CreditStatus status) {
        return switch (status) {
            case APPROVED -> "одобрена";
            case REJECTED -> "отклонена";
            case PENDING -> "на рассмотрении";
        };
    }
}
