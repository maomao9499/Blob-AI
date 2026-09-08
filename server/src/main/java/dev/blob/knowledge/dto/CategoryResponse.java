package dev.blob.knowledge.dto;

import java.time.LocalDateTime;

public record CategoryResponse(long id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {}
