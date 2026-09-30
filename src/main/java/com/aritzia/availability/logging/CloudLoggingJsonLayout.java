package com.aritzia.availability.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// One JSON object per line on stdout; Cloud Logging reads "severity", "message" and "time" natively.
public class CloudLoggingJsonLayout extends LayoutBase<ILoggingEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String doLayout(ILoggingEvent event) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("time", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        entry.put("severity", severity(event.getLevel()));
        entry.put("message", message(event));
        entry.put("logger", event.getLoggerName());
        entry.put("thread", event.getThreadName());
        String requestId = event.getMDCPropertyMap().get("requestId");
        if (requestId != null) {
            entry.put("requestId", requestId);
        }
        try {
            return MAPPER.writeValueAsString(entry) + System.lineSeparator();
        } catch (JsonProcessingException e) {
            return "{\"severity\":\"ERROR\",\"message\":\"log serialization failed\"}" + System.lineSeparator();
        }
    }

    // Stack traces go in the message so Cloud Error Reporting can group them.
    private static String message(ILoggingEvent event) {
        IThrowableProxy throwable = event.getThrowableProxy();
        return throwable == null
                ? event.getFormattedMessage()
                : event.getFormattedMessage() + System.lineSeparator() + ThrowableProxyUtil.asString(throwable);
    }

    private static String severity(Level level) {
        return switch (level.toInt()) {
            case Level.ERROR_INT -> "ERROR";
            case Level.WARN_INT -> "WARNING";
            case Level.INFO_INT -> "INFO";
            default -> "DEBUG";
        };
    }
}
