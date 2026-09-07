package dev.blob.journal.dto;

import java.time.LocalDate;
import java.util.Set;

public record JournalQuery(
        String keyword,
        String entryType,
        Long tagId,
        LocalDate startDate,
        LocalDate endDate,
        Integer page,
        Integer pageSize
) {
    private static final Set<String> ENTRY_TYPES = Set.of("LEARNING", "LIFE");

    public JournalQuery {
        keyword = normalize(keyword);
        entryType = normalize(entryType);
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : pageSize;

        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be at least 1");
        }
        pageSize = Math.min(pageSize, 100);
        if (entryType != null && !ENTRY_TYPES.contains(entryType)) {
            throw new IllegalArgumentException("entryType must be LEARNING or LIFE");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
