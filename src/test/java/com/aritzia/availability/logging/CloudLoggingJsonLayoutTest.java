package com.aritzia.availability.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CloudLoggingJsonLayoutTest {

    private final CloudLoggingJsonLayout layout = new CloudLoggingJsonLayout();
    private final Logger logger = new LoggerContext().getLogger("test");

    private JsonNode render(Level level, String message, Throwable throwable) throws Exception {
        LoggingEvent event = new LoggingEvent("fqcn", logger, level, message, throwable, null);
        event.setMDCPropertyMap(Map.of("requestId", "req-42"));
        return new ObjectMapper().readTree(layout.doLayout(event));
    }

    @Test
    void mapsWarnToCloudLoggingWarningSeverity() throws Exception {
        JsonNode json = render(Level.WARN, "Invalid productId rejected", null);

        assertThat(json.get("severity").asText()).isEqualTo("WARNING");
        assertThat(json.get("message").asText()).isEqualTo("Invalid productId rejected");
        assertThat(json.get("requestId").asText()).isEqualTo("req-42");
        assertThat(json.has("time")).isTrue();
    }

    @Test
    void includesStackTraceInMessageForErrors() throws Exception {
        JsonNode json = render(Level.ERROR, "Unexpected server error", new IllegalStateException("boom"));

        assertThat(json.get("severity").asText()).isEqualTo("ERROR");
        assertThat(json.get("message").asText()).contains("IllegalStateException").contains("boom");
    }

    @Test
    void escapesQuotesAndNewlinesSoEachEntryStaysValidJson() throws Exception {
        JsonNode json = render(Level.INFO, "value \"quoted\"\nsecond line", null);

        assertThat(json.get("message").asText()).isEqualTo("value \"quoted\"\nsecond line");
    }
}
