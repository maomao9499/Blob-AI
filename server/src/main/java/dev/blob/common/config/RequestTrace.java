package dev.blob.common.config;

import java.util.UUID;

public final class RequestTrace {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private RequestTrace() {
    }

    public static void set(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String currentId() {
        String traceId = TRACE_ID.get();
        if (traceId == null) {
            traceId = newTraceId();
            TRACE_ID.set(traceId);
        }
        return traceId;
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
