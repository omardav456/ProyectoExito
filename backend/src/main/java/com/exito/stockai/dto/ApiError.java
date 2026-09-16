package com.exito.stockai.dto;

public record ApiError(
    int status,
    String error,
    java.util.List<String> detalles,
    java.time.OffsetDateTime timestamp
) {
    public static ApiError of(int status, String error, java.util.List<String> detalles) {
        return new ApiError(status, error, detalles, java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
    }
}