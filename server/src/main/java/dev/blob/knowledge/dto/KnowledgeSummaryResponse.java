package dev.blob.knowledge.dto;

import java.time.LocalDateTime;
import java.util.List;
import dev.blob.tag.dto.TagResponse;

public record KnowledgeSummaryResponse(long id, String title, String summary, String excerpt, CategorySummaryResponse category,
        List<TagResponse> tags, LocalDateTime createdAt, LocalDateTime updatedAt) {}
