package dev.blob.journal.dto;

import dev.blob.tag.dto.TagResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record JournalSummaryResponse(
        Long id,
        String title,
        String entryType,
        LocalDate entryDate,
        String aiSummary,
        List<TagResponse> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public JournalSummaryResponse {
        tags = List.copyOf(tags);
    }
}
