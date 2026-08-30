package ru.creditbank.credit.operations.credit.create.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.repository.CreditRepository;
import ru.creditbank.credit.operations.support.JwtTestTokenFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CreditApplicationControllerIntegrationTest {
    private static final String ENDPOINT = "/credit-service/api/v1/credit/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CreditRepository creditRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void createApplication_withValidJwtAndValidData_savesToDbAndReturnsCreatedApplication() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = generateToken(userId, "ivanov@example.com");
        Map<String, Object> requestBody = Map.of(
                "fullName", "Иванов Иван Иванович",
                "requestedAmount", 50000,
                "termMonths", 12
        );

        String responseJson = mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();

        UUID createdId = UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());

        CreditEntity saved = creditRepository.findById(createdId).orElseThrow();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getUserEmail()).isEqualTo("ivanov@example.com");
        assertThat(saved.getUserFullName()).isEqualTo("Иванов Иван Иванович");
        assertThat(saved.getRequestedAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(saved.getTermMonths()).isEqualTo(12);
        assertThat(saved.getStatus()).isEqualTo(CreditStatus.PENDING);
        assertThat(saved.getCreationDate()).isNotNull();
        assertThat(saved.getLastUpdated()).isNotNull();
    }

    @Test
    void createApplication_withoutJwt_returnsUnauthorized() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "fullName", "Иванов Иван Иванович",
                "requestedAmount", 50000,
                "termMonths", 12
        );

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createApplication_withInvalidJwt_returnsUnauthorized() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "fullName", "Иванов Иван Иванович",
                "requestedAmount", 50000,
                "termMonths", 12
        );

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer invalid.token.value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createApplication_withInvalidData_returnsBadRequest() throws Exception {
        String token = generateToken(UUID.randomUUID(), "user@example.com");
        Map<String, Object> requestBody = Map.of(
                "fullName", "Ив",
                "requestedAmount", BigDecimal.valueOf(-1),
                "termMonths", 0
        );

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createApplication_withMissingFields_returnsBadRequest() throws Exception {
        String token = generateToken(UUID.randomUUID(), "user@example.com");

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private String generateToken(UUID userId, String email) {
        return JwtTestTokenFactory.generateToken(jwtSecret, userId, email, null);
    }
}
