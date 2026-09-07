package dev.blob.common.api;

import dev.blob.common.config.RequestTrace;

public record ApiResponse<T>(String code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data, RequestTrace.currentId());
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(code, message, data, RequestTrace.currentId());
    }
}
