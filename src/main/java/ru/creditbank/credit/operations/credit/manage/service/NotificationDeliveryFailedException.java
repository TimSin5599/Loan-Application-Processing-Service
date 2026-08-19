package ru.creditbank.credit.operations.credit.manage.service;

import java.util.UUID;

public class NotificationDeliveryFailedException extends RuntimeException {

    public NotificationDeliveryFailedException(UUID creditApplicationId, Throwable cause) {
        super("Не удалось отправить уведомление по заявке " + creditApplicationId, cause);
    }
}
