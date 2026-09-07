package dev.blob.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 20) String color
) {
}
