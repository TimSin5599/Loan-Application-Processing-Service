package ru.creditbank.credit.operations.credit.manage.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import ru.creditbank.credit.operations.config.GatewayAuthenticationFilter;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.repository.CreditRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * response.sendError(403) в SecurityConfig вызывает внутренний forward на /error,
 * который заново проходит через цепочку фильтров Spring Security — MockMvc этот forward
 * не воспроизводит, поэтому регрессию (403 незаметно подменяется на 401) видно только
 * на реальном поднятом сервере. Отсюда RANDOM_PORT + TestRestTemplate вместо MockMvc.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CreditManageSecurityRealServerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CreditRepository creditRepository;

    @BeforeEach
    void supportPatchMethod() {
        // Стандартный JDK HttpURLConnection, который использует TestRestTemplate по умолчанию,
        // не умеет в PATCH-запросы.
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void updateStatus_asNonManager_returnsForbidden_overRealHttp() {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(GatewayAuthenticationFilter.USER_ID_HEADER, credit.getUserId().toString());
        HttpEntity<String> request = new HttpEntity<>("{\"status\":\"APPROVED\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/credit-service/api/credit/" + credit.getId() + "/status",
                HttpMethod.PATCH, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private CreditEntity newCredit(UUID ownerId) {
        LocalDateTime now = LocalDateTime.now();
        return CreditEntity.builder()
                .userId(ownerId)
                .userEmail("ivanov@example.com")
                .userFullName("Иванов Иван Иванович")
                .requestedAmount(BigDecimal.valueOf(50000))
                .termMonths(12)
                .status(CreditStatus.PENDING)
                .creationDate(now)
                .lastUpdated(now)
                .build();
    }
}
