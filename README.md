# Учебное задание 2: Сервис обработки кредитных заявок. Подача онлайн-заявки на кредит

### Цель

Создать микросервис: credit-service, который:
- принимает заявку на кредит от API Gateway, сохраняет её в БД и возвращает результат.

---

## 📌 **Функциональность**

1. принимать запрос от API Gateway (с JWT в headers).
2. валидировать JWT, извлекать из него user_id
3. валидировать данные заявки.
4. сохранять в таблицу БД credit_operation с полями:

| Поле             | Тип          | Описание             |
|------------------|--------------|----------------------|
| id               | UUID         | Уникальный ID записи |
| user_id          | UUID         | ID пользователя      |
| user_full_name   | VARCHAR(100) | ФИО                  |
| requested_amount | DECIMAL      | Сумма кредита        |
| term_months      | INTEGER      | Срок (месяцы)        |
| status           | VARCHAR(20)  | Статус               |
| creation_date    | TIMESTAMP    | Дата создания        |
| last_updated     | TIMESTAMP    | Дата обновления      |

---

## 🛠 **Технологии**

- Java 17
- Spring Boot 3.x
- Spring Data JPA (Hibernate)
- PostgreSQL / H2 (для разработки)
- Lombok

---

## 📂 **Структура проекта**

credit-operations  
├── src  
│ ├── main  
│ │ ├── java  
│ │ │ └── ru.creditbank.credit.operations  
│ │ │ │ ├── config # Security  
│ │ │ │ ├── credit    
│ │ │ │ │ ├── create # CreditApplicationController  
│ │ │ │ │ │ ├── rest # CreditApplicationController  
│ │ │ │ │ │ │ ├── dto # Request/Response  
│ │ │ │ │ │ ├── service # CreditCreateUseCase  
│ │ │ │ │ │ ├── dao              
│ │ │ │ │ │ │ ├── entity # CreditEntity  
│ │ │ │ │ │ │ ├── repository # CreditRepository  
│ │ │ │ │ │ │ ├── service # CreditProvider  
│ │ │ │ └── CreditAppApplication.java  
│ │ └── resources  
│ │ ├── application.yml  
│ └── test # Тесты  

---

## 🔐 **API Endpoints**
[open-api](credit-open-api.yaml)
1. Подача заявки
---

## 🧪 Тестирование

1. Через Postman:
    - отправить POST-запрос с JWT и данными заявки.
    - проверить, что заявка сохранилась в БД.
2. Интеграционные тесты:
    - проверить, что заявка сохранилась в БД.
    - проверить обработку ошибок.
3. Unit тесты:
    - проверить правильность валидации полей
---

## 📌 Дополнительные задания