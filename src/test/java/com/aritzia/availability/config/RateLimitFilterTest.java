package com.aritzia.availability.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter(Jackson2ObjectMapperBuilder.json().build());

    private MockHttpServletResponse send(String clientIp) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/availability/10001");
        request.setRemoteAddr(clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void returns429WithSharedErrorBodyOnceBurstCapacityIsUsed() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertThat(send("203.0.113.7").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse throttled = send("203.0.113.7");

        assertThat(throttled.getStatus()).isEqualTo(429);
        assertThat(throttled.getHeader("Retry-After")).isEqualTo("1");
        assertThat(throttled.getContentAsString())
                .contains("\"status\":429")
                .contains("\"error\":\"Too Many Requests\"")
                .contains("\"timestamp\":");
    }

    @Test
    void limitsAreTrackedPerClient() throws Exception {
        for (int i = 0; i < 20; i++) {
            send("203.0.113.7");
        }

        assertThat(send("203.0.113.7").getStatus()).isEqualTo(429);
        assertThat(send("198.51.100.9").getStatus()).isEqualTo(200);
    }
}
