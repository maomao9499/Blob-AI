package dev.blob.journal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record JournalUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String contentMd,
        @NotBlank @Pattern(regexp = "LEARNING|LIFE") String entryType,
        @NotNull LocalDate entryDate,
        @NotNull List<Long> tagIds
) {
    public JournalUpdateRequest {
        tagIds = tagIds == null ? null : List.copyOf(tagIds);
    }
}
