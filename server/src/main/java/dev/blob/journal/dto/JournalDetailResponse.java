package dev.blob.journal.dto;

import dev.blob.tag.dto.TagResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record JournalDetailResponse(
        Long id,
        String title,
        String contentMd,
        String entryType,
        LocalDate entryDate,
        String aiSummary,
        List<TagResponse> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public JournalDetailResponse {
        tags = List.copyOf(tags);
    }
}
