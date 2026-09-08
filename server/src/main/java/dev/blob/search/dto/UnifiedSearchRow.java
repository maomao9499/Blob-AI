package dev.blob.search.dto;

import java.time.LocalDateTime;

/** Read-only projection of one globally paginated search result. */
public record UnifiedSearchRow(String sourceType, Long sourceId, String title, String contentMd,
                               LocalDateTime updatedAt) {
}
