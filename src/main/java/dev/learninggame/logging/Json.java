package dev.learninggame.logging;

import java.util.Map;
import java.util.stream.Collectors;

final class Json {
    private Json() {
    }

    static String object(Map<String, ?> values) {
        return values.entrySet().stream()
                .map(entry -> quote(entry.getKey()) + ":" + value(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String value(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, ?> strings = (Map<String, ?>) map;
            return object(strings);
        }
        return quote(value.toString());
    }

    static String stringField(String json, String field) {
        String marker = quote(field) + ":";
        int start = json.indexOf(marker);
        if (start < 0) return null;
        start += marker.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length() || json.charAt(start) != '"') return null;
        StringBuilder result = new StringBuilder();
        for (int i = start + 1; i < json.length(); i++) {
            char current = json.charAt(i);
            if (current == '"') return result.toString();
            if (current == '\\' && i + 1 < json.length()) {
                char escaped = json.charAt(++i);
                result.append(escaped == 'n' ? '\n' : escaped);
            } else {
                result.append(current);
            }
        }
        return null;
    }

    static boolean booleanField(String json, String field) {
        String marker = quote(field) + ":";
        int start = json.indexOf(marker);
        if (start < 0) throw new IllegalArgumentException("Envelope has no " + field + " field");
        start += marker.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (json.startsWith("true", start)) return true;
        if (json.startsWith("false", start)) return false;
        throw new IllegalArgumentException("Envelope field " + field + " is not boolean");
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}

