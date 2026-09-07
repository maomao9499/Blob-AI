package dev.blob.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagUpdateRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 20) String color
) {
}
