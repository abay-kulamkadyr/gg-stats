package com.abe.gg_stats.dto.response;

import java.time.Instant;
import java.util.Map;

public class ActionResponse {

    private final boolean success;

    private final String message;

    private final Map<String, Object> details;

    private final Instant timestamp;

    private ActionResponse(boolean success, String message, Map<String, Object> details) {
        this.success = success;
        this.message = message;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public static ActionResponse success(String message) {
        return new ActionResponse(true, message, Map.of());
    }

    public static ActionResponse success(String message, Map<String, Object> details) {
        return new ActionResponse(true, message, details);
    }

    public static ActionResponse error(String message) {
        return new ActionResponse(false, message, Map.of());
    }

    public static ActionResponse error(String message, Map<String, Object> details) {
        return new ActionResponse(false, message, details);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}


