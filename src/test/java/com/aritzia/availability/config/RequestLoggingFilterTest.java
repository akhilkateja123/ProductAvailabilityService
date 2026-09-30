package com.aritzia.availability.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    private MockHttpServletResponse send(String incomingRequestId, AtomicReference<String> mdcDuringRequest)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/availability/10001");
        if (incomingRequestId != null) {
            request.addHeader("X-Request-Id", incomingRequestId);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> mdcDuringRequest.set(MDC.get("requestId")));
        return response;
    }

    @Test
    void generatesRequestIdWhenNoneSupplied() throws Exception {
        AtomicReference<String> mdc = new AtomicReference<>();

        MockHttpServletResponse response = send(null, mdc);

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(mdc.get()).isEqualTo(response.getHeader("X-Request-Id"));
    }

    @Test
    void reusesSafeIncomingRequestId() throws Exception {
        AtomicReference<String> mdc = new AtomicReference<>();

        MockHttpServletResponse response = send("checkout-abc-123", mdc);

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("checkout-abc-123");
        assertThat(mdc.get()).isEqualTo("checkout-abc-123");
    }

    @Test
    void replacesUnsafeIncomingRequestId() throws Exception {
        MockHttpServletResponse response = send("bad id\nINJECTED", new AtomicReference<>());

        assertThat(response.getHeader("X-Request-Id")).doesNotContain("INJECTED");
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        send(null, new AtomicReference<>());

        assertThat(MDC.get("requestId")).isNull();
    }
}
