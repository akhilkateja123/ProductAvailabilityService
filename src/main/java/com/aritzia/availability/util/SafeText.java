package com.aritzia.availability.util;

// Caller-supplied text that ends up in logs or error messages: bounded length, no control characters.
public final class SafeText {

    private static final int MAX_LENGTH = 50;

    private SafeText() {
    }

    public static String of(String raw) {
        if (raw == null) {
            return "null";
        }
        String cleaned = raw.replaceAll("\\p{Cntrl}", "_");
        return cleaned.length() <= MAX_LENGTH ? cleaned : cleaned.substring(0, MAX_LENGTH) + "...";
    }
}
