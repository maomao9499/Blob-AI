package dev.blob.search.dto;

import java.util.Set;

public record UnifiedSearchQuery(String keyword, String sourceType, Long tagId, Integer page, Integer pageSize) {
    private static final Set<String> SOURCE_TYPES = Set.of("ALL", "JOURNAL", "KNOWLEDGE");

    public UnifiedSearchQuery {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        sourceType = sourceType == null ? "ALL" : sourceType;
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : pageSize;
        if (!SOURCE_TYPES.contains(sourceType)) {
            throw new IllegalArgumentException("sourceType must be ALL, JOURNAL or KNOWLEDGE");
        }
        if (tagId != null && tagId < 1) {
            throw new IllegalArgumentException("tagId must be positive");
        }
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("page must be positive and pageSize must be between 1 and 100");
        }
    }

    public String likePattern() {
        return keyword == null ? null : "%" + keyword.replace("!", "!!")
                .replace("%", "!%").replace("_", "!_") + "%";
    }
}
