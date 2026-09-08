package dev.blob.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryWriteRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 500) String description
) {
    public CategoryWriteRequest {
        name = name == null ? null : name.trim();
        description = description == null || description.isBlank() ? null : description.trim();
    }
}
