package ru.creditbank.credit.operations.credit.manage.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.creditbank.credit.operations.config.Roles;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.repository.CreditRepository;
import ru.creditbank.credit.operations.support.JwtTestTokenFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CreditManageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CreditRepository creditRepository;

    @MockBean
    private JavaMailSender mailSender;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void getApplication_asManager_returnsDetails() throws Exception {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));
        String token = token(UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER);

        mockMvc.perform(get("/credit-service/api/credit/{id}", credit.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(credit.getId().toString()))
                .andExpect(jsonPath("$.userInfo.email").value(credit.getUserEmail()))
                .andExpect(jsonPath("$.loanDetails.requestedAmount").value(50000))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getApplication_asOwner_returnsDetails() throws Exception {
        UUID ownerId = UUID.randomUUID();
        CreditEntity credit = creditRepository.save(newCredit(ownerId));
        String token = token(ownerId, credit.getUserEmail(), null);

        mockMvc.perform(get("/credit-service/api/credit/{id}", credit.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getApplication_asUnrelatedUser_returnsForbidden() throws Exception {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));
        String token = token(UUID.randomUUID(), "stranger@example.com", null);

        mockMvc.perform(get("/credit-service/api/credit/{id}", credit.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getApplication_withoutJwt_returnsUnauthorized() throws Exception {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));

        mockMvc.perform(get("/credit-service/api/credit/{id}", credit.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getApplication_unknownId_returnsNotFound() throws Exception {
        String token = token(UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER);

        mockMvc.perform(get("/credit-service/api/credit/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_asManager_updatesDbAndSendsNotification() throws Exception {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));
        String token = token(UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER);
        Map<String, Object> requestBody = Map.of(
                "status", "APPROVED",
                "managerComment", "Заявка одобрена",
                "interestRate", 15.5
        );

        mockMvc.perform(patch("/credit-service/api/credit/{id}/status", credit.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());

        CreditEntity updated = creditRepository.findById(credit.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CreditStatus.APPROVED);
        assertThat(updated.getManagerComment()).isEqualTo("Заявка одобрена");
        assertThat(updated.getInterestRate()).isEqualByComparingTo(BigDecimal.valueOf(15.5));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void updateStatus_asNonManager_returnsForbidden() throws Exception {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));
        String token = token(credit.getUserId(), credit.getUserEmail(), null);
        Map<String, Object> requestBody = Map.of("status", "APPROVED");

        mockMvc.perform(patch("/credit-service/api/credit/{id}/status", credit.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_withInvalidStatus_returnsBadRequest() throws Exception {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));
        String token = token(UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER);
        Map<String, Object> requestBody = Map.of("status", "PENDING");

        mockMvc.perform(patch("/credit-service/api/credit/{id}/status", credit.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_unknownId_returnsNotFound() throws Exception {
        String token = token(UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER);
        Map<String, Object> requestBody = Map.of("status", "APPROVED");

        mockMvc.perform(patch("/credit-service/api/credit/{id}/status", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound());
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

    private String token(UUID userId, String email, String role) {
        return JwtTestTokenFactory.generateToken(jwtSecret, userId, email, role);
    }
}
