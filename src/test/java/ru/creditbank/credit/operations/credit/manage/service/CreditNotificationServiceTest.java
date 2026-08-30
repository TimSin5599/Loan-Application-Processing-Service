package ru.creditbank.credit.operations.credit.manage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreditNotificationServiceTest {
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private CreditNotificationService creditNotificationService;

    @Test
    void notifyStatusChange_approved_buildsExpectedMessage() {
        CreditEntity credit = credit(CreditStatus.APPROVED, "Заявка одобрена, деньги поступят в течение часа");

        creditNotificationService.notifyStatusChange(credit);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getTo()).containsExactly("ivanov@example.com");
        assertThat(sent.getSubject()).isEqualTo("Ваша кредитная заявка #" + credit.getId());
        assertThat(sent.getText())
                .contains("Иванов Иван Иванович")
                .contains("одобрена")
                .contains("Заявка одобрена, деньги поступят в течение часа");
    }

    @Test
    void notifyStatusChange_rejected_buildsExpectedMessage() {
        CreditEntity credit = credit(CreditStatus.REJECTED, "Недостаточный кредитный рейтинг");

        creditNotificationService.notifyStatusChange(credit);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getText())
                .contains("отклонена")
                .contains("Недостаточный кредитный рейтинг");
    }

    @Test
    void notifyStatusChange_mailSendFails_throwsNotificationDeliveryFailedException() {
        CreditEntity credit = credit(CreditStatus.APPROVED, "Одобрено");
        doThrow(new MailSendException("SMTP недоступен")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> creditNotificationService.notifyStatusChange(credit))
                .isInstanceOf(NotificationDeliveryFailedException.class)
                .hasMessageContaining(credit.getId().toString());
    }

    private CreditEntity credit(CreditStatus status, String managerComment) {
        return CreditEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .userEmail("ivanov@example.com")
                .userFullName("Иванов Иван Иванович")
                .status(status)
                .managerComment(managerComment)
                .creationDate(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
