package ru.creditbank.credit.operations.credit.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.creditbank.credit.operations.credit.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.dao.repository.CreditRepository;
import ru.creditbank.credit.operations.support.JwtTestTokenFactory;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CreditAutoScoringIntegrationTest {
    private static final String ENDPOINT = "/credit-service/api/v1/credit/";
    private static final String PAYMENT_HISTORY_PATH = "/loan-management-service/internal/users/.*/payment-history";
    private static final String ISSUE_LOAN_PATH = "/loan-management-service/internal/loans";

    private static WireMockServer loanManagementService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CreditRepository creditRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private JavaMailSender mailSender;

    @Value("${jwt.secret}")
    private String jwtSecret;

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
        registry.add("services.loan-management.internal-api-key", () -> "test-internal-key");
    }

    @BeforeEach
    void resetStub() {
        loanManagementService.resetAll();
        circuitBreakerRegistry.circuitBreaker("loanManagementService").reset();
    }

    @Test
    void createApplication_goodPaymentHistory_autoApprovesIssuesLoanAndNotifies() throws Exception {
        UUID loanId = UUID.randomUUID();
        stubPaymentHistory("""
                {"totalLoans":3,"activeLoans":1,"hasActiveOverdue":false,"totalOutstandingDebt":200000}""");
        loanManagementService.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlMatching(ISSUE_LOAN_PATH))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"loanId\":\"" + loanId + "\",\"status\":\"ACTIVE\"}")));
        loanManagementService.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(
                        urlMatching("/loan-management-service/internal/loans/" + loanId + "/schedule"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"loanId\":\"" + loanId + "\",\"installmentsCreated\":12}")));

        UUID createdId = createApplication(300_000, 12);

        CreditEntity saved = creditRepository.findById(createdId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(CreditStatus.APPROVED);
        assertThat(saved.getInterestRate()).isNotNull();

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void createApplication_activeOverdueInHistory_autoRejectsAndNotifiesWithoutIssuingLoan() throws Exception {
        stubPaymentHistory("""
                {"totalLoans":2,"activeLoans":1,"hasActiveOverdue":true,"totalOutstandingDebt":100000}""");

        UUID createdId = createApplication(300_000, 12);

        CreditEntity saved = creditRepository.findById(createdId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(CreditStatus.REJECTED);
        assertThat(saved.getManagerComment()).isNotBlank();

        verify(mailSender).send(any(SimpleMailMessage.class));
        loanManagementService.verify(0, postRequestedFor(urlMatching(ISSUE_LOAN_PATH)));
    }

    @Test
    void createApplication_noCreditHistory_leavesPendingForManualReview() throws Exception {
        stubPaymentHistory("""
                {"totalLoans":0,"activeLoans":0,"hasActiveOverdue":false,"totalOutstandingDebt":0}""");

        UUID createdId = createApplication(300_000, 12);

        CreditEntity saved = creditRepository.findById(createdId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(CreditStatus.PENDING);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    private void stubPaymentHistory(String body) {
        loanManagementService.stubFor(get(urlMatching(PAYMENT_HISTORY_PATH))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private UUID createApplication(int amount, int termMonths) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "fullName", "Иванов Иван Иванович",
                "requestedAmount", amount,
                "termMonths", termMonths
        );

        String token = JwtTestTokenFactory.generateToken(jwtSecret, UUID.randomUUID(), "ivanov@example.com", null);
        String responseJson = mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());
    }
}
