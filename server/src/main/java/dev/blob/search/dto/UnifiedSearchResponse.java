package dev.blob.search.dto;

import dev.blob.tag.dto.TagResponse;

import java.time.LocalDateTime;
import java.util.List;

public record UnifiedSearchResponse(String sourceType, Long sourceId, String title, String excerpt,
                                    List<TagResponse> tags, LocalDateTime updatedAt) {
}
