package ru.creditbank.credit.operations.credit.manage.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.creditbank.credit.operations.config.GatewayAuthenticationFilter;
import ru.creditbank.credit.operations.config.Roles;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.repository.CreditRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CreditManageControllerIntegrationTest {

    private static final String INTERNAL_API_KEY = "test-internal-key";
    private static WireMockServer loanManagementService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CreditRepository creditRepository;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeAll
    static void startLoanManagementServiceStub() {
        loanManagementService = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        loanManagementService.start();
    }

    @AfterAll
    static void stopLoanManagementServiceStub() {
        loanManagementService.stop();
    }

    @DynamicPropertySource
    static void loanManagementServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("services.loan-management.base-url", () -> "http://localhost:" + loanManagementService.port());
        registry.add("services.loan-management.internal-api-key", () -> INTERNAL_API_KEY);
    }

    @BeforeEach
    void resetStub() {
        loanManagementService.resetAll();
    }

    @Test
    void getApplication_asManager_returnsDetails() throws Exception {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));

        mockMvc.perform(asUser(get("/credit-service/api/credit/{id}", credit.getId()),
                        UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER))
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

        mockMvc.perform(asUser(get("/credit-service/api/credit/{id}", credit.getId()),
                        ownerId, credit.getUserEmail(), null))
                .andExpect(status().isOk());
    }

    @Test
    void getApplication_asAnyAuthenticatedUser_returnsDetails() throws Exception {
        // Доступ к заявке ограничивается на уровне apigateway, а не в этом сервисе —
        // сюда долетают уже авторизованные запросы.
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));

        mockMvc.perform(asUser(get("/credit-service/api/credit/{id}", credit.getId()),
                        UUID.randomUUID(), "someone-else@example.com", null))
                .andExpect(status().isOk());
    }

    @Test
    void getApplication_withoutUserHeader_returnsUnauthorized() throws Exception {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));

        mockMvc.perform(get("/credit-service/api/credit/{id}", credit.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getApplication_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(asUser(get("/credit-service/api/credit/{id}", UUID.randomUUID()),
                        UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_asManager_updatesDbAndSendsNotification() throws Exception {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));
        Map<String, Object> requestBody = Map.of(
                "status", "APPROVED",
                "managerComment", "Заявка одобрена",
                "interestRate", 15.5
        );
        loanManagementService.stubFor(post(urlEqualTo("/loan-management-service/internal/loans"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"loanId\":\"" + UUID.randomUUID() + "\",\"status\":\"ACTIVE\"}")));

        mockMvc.perform(asUser(patch("/credit-service/api/credit/{id}/status", credit.getId()),
                        UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());

        CreditEntity updated = creditRepository.findById(credit.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CreditStatus.APPROVED);
        assertThat(updated.getManagerComment()).isEqualTo("Заявка одобрена");
        assertThat(updated.getInterestRate()).isEqualByComparingTo(BigDecimal.valueOf(15.5));

        verify(mailSender).send(any(SimpleMailMessage.class));
        loanManagementService.verify(postRequestedFor(urlEqualTo("/loan-management-service/internal/loans"))
                .withHeader("X-Internal-Api-Key", equalTo(INTERNAL_API_KEY))
                .withRequestBody(matchingJsonPath("$.creditApplicationId", equalTo(credit.getId().toString()))));
    }

    @Test
    void updateStatus_asManager_loanIssuanceFails_returnsBadGatewayAndKeepsCreditPending() throws Exception {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));
        Map<String, Object> requestBody = Map.of("status", "APPROVED");
        loanManagementService.stubFor(post(urlEqualTo("/loan-management-service/internal/loans"))
                .willReturn(aResponse().withStatus(500)));

        mockMvc.perform(asUser(patch("/credit-service/api/credit/{id}/status", credit.getId()),
                        UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));

        CreditEntity unchanged = creditRepository.findById(credit.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(CreditStatus.PENDING);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void updateStatus_asNonManager_returnsForbidden() throws Exception {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));
        Map<String, Object> requestBody = Map.of("status", "APPROVED");

        mockMvc.perform(asUser(patch("/credit-service/api/credit/{id}/status", credit.getId()),
                        credit.getUserId(), credit.getUserEmail(), null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_withInvalidStatus_returnsBadRequest() throws Exception {
        CreditEntity credit = creditRepository.save(newCredit(UUID.randomUUID()));
        Map<String, Object> requestBody = Map.of("status", "PENDING");

        mockMvc.perform(asUser(patch("/credit-service/api/credit/{id}/status", credit.getId()),
                        UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_unknownId_returnsNotFound() throws Exception {
        Map<String, Object> requestBody = Map.of("status", "APPROVED");

        mockMvc.perform(asUser(patch("/credit-service/api/credit/{id}/status", UUID.randomUUID()),
                        UUID.randomUUID(), "manager@example.com", Roles.CREDIT_MANAGER)
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

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder,
                                                  UUID userId, String email, String role) {
        builder.header(GatewayAuthenticationFilter.USER_ID_HEADER, userId.toString());
        if (email != null) {
            builder.header(GatewayAuthenticationFilter.USER_EMAIL_HEADER, email);
        }
        if (role != null) {
            builder.header(GatewayAuthenticationFilter.USER_ROLE_HEADER, role);
        }
        return builder;
    }
}
