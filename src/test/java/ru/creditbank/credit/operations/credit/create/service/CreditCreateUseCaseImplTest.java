package ru.creditbank.credit.operations.credit.create.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.creditbank.credit.operations.credit.create.dao.entity.CreditEntity;
import ru.creditbank.credit.operations.credit.create.dao.entity.CreditStatus;
import ru.creditbank.credit.operations.credit.create.dao.service.CreditProvider;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationRequest;
import ru.creditbank.credit.operations.credit.create.rest.dto.CreditApplicationResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCreateUseCaseImplTest {

    @Mock
    private CreditProvider creditProvider;

    @InjectMocks
    private CreditCreateUseCaseImpl creditCreateUseCase;

    @Test
    void createApplication_savesEntityWithUserIdAndPendingStatus_andReturnsResponse() {
        UUID userId = UUID.randomUUID();
        UUID savedId = UUID.randomUUID();
        LocalDateTime creationDate = LocalDateTime.now();
        CreditApplicationRequest request = new CreditApplicationRequest(
                "Иванов Иван Иванович", BigDecimal.valueOf(50000), 12);

        CreditEntity persisted = CreditEntity.builder()
                .id(savedId)
                .userId(userId)
                .userFullName(request.fullName())
                .requestedAmount(request.requestedAmount())
                .termMonths(request.termMonths())
                .status(CreditStatus.PENDING)
                .creationDate(creationDate)
                .lastUpdated(creationDate)
                .build();

        when(creditProvider.save(any(CreditEntity.class))).thenReturn(persisted);

        CreditApplicationResponse response = creditCreateUseCase.createApplication(userId, request);

        ArgumentCaptor<CreditEntity> captor = ArgumentCaptor.forClass(CreditEntity.class);
        verify(creditProvider).save(captor.capture());
        CreditEntity toSave = captor.getValue();

        assertThat(toSave.getUserId()).isEqualTo(userId);
        assertThat(toSave.getUserFullName()).isEqualTo(request.fullName());
        assertThat(toSave.getRequestedAmount()).isEqualByComparingTo(request.requestedAmount());
        assertThat(toSave.getTermMonths()).isEqualTo(request.termMonths());
        assertThat(toSave.getStatus()).isEqualTo(CreditStatus.PENDING);

        assertThat(response.id()).isEqualTo(savedId);
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.createdAt()).isNotNull();
    }
}