package ru.creditbank.credit.operations.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraceIdFilterTest {
    private final TraceIdFilter filter = new TraceIdFilter();

    TraceIdFilterTest() {
        ReflectionTestUtils.setField(filter, "serviceName", "credit-operations");
    }

    @Test
    void doFilterInternal_noIncomingTraceId_generatesOneAndFillsMdcDuringChain() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        Mockito.doAnswer(invocation -> {
            assertThat(MDC.get("traceId")).isNotBlank();
            assertThat(MDC.get("spanId")).isNotBlank();
            assertThat(MDC.get("service")).isEqualTo("credit-operations");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        verify(response).setHeader(org.mockito.ArgumentMatchers.eq("X-Trace-Id"), any());
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void doFilterInternal_incomingTraceId_isEchoedBackUnchanged() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Trace-Id")).thenReturn("caller-trace-id");

        filter.doFilter(request, response, chain);

        verify(response).setHeader("X-Trace-Id", "caller-trace-id");
        assertThat(MDC.get("traceId")).isNull();
    }
}
