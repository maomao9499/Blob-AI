package dev.blob.knowledge.dto;

import java.time.LocalDateTime;
import java.util.List;
import dev.blob.tag.dto.TagResponse;

public record KnowledgeDetailResponse(long id, String title, String contentMd, String summary, CategorySummaryResponse category,
        List<TagResponse> tags, Long sourceJournalId, String sourceJournalTitle,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
