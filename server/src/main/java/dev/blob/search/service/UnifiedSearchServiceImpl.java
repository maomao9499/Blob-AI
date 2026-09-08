package dev.blob.search.service;

import dev.blob.common.api.PageResponse;
import dev.blob.journal.service.SearchHistoryRepository;
import dev.blob.search.dto.UnifiedSearchQuery;
import dev.blob.search.dto.UnifiedSearchResponse;
import dev.blob.search.dto.UnifiedSearchRow;
import dev.blob.search.mapper.UnifiedSearchMapper;
import dev.blob.tag.dto.TagResponse;
import dev.blob.tag.service.TagService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UnifiedSearchServiceImpl implements UnifiedSearchService {
    private final UnifiedSearchMapper mapper;
    private final TagService tagService;
    private final SearchHistoryRepository history;

    public UnifiedSearchServiceImpl(UnifiedSearchMapper mapper, TagService tagService,
                                    SearchHistoryRepository history) {
        this.mapper = mapper;
        this.tagService = tagService;
        this.history = history;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UnifiedSearchResponse> search(UnifiedSearchQuery query) {
        long total = mapper.countSearch(query);
        long offset = (query.page() - 1L) * query.pageSize();
        List<UnifiedSearchRow> rows = total == 0 ? List.of() : mapper.search(query, offset, query.pageSize());
        List<Long> journalIds = idsFor(rows, "JOURNAL");
        List<Long> knowledgeIds = idsFor(rows, "KNOWLEDGE");
        Map<Long, List<TagResponse>> journalTags = journalIds.isEmpty()
                ? Map.of() : tagService.findByJournalIds(journalIds);
        Map<Long, List<TagResponse>> knowledgeTags = knowledgeIds.isEmpty()
                ? Map.of() : tagService.findByKnowledgeIds(knowledgeIds);
        List<UnifiedSearchResponse> items = rows.stream().map(row -> new UnifiedSearchResponse(
                row.sourceType(), row.sourceId(), row.title(), excerpt(row.contentMd()),
                ("JOURNAL".equals(row.sourceType()) ? journalTags : knowledgeTags)
                        .getOrDefault(row.sourceId(), List.of()), row.updatedAt())).toList();
        if (query.keyword() != null) {
            try {
                history.record(query.keyword());
            } catch (DataAccessException ignored) {
                // Optional Redis history must not discard authoritative MySQL results.
            }
        }
        return new PageResponse<>(items, total, query.page(), query.pageSize());
    }

    private List<Long> idsFor(List<UnifiedSearchRow> rows, String type) {
        return rows.stream().filter(row -> type.equals(row.sourceType()))
                .map(UnifiedSearchRow::sourceId).toList();
    }

    private String excerpt(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.codePointCount(0, normalized.length()) <= 200 ? normalized
                : normalized.substring(0, normalized.offsetByCodePoints(0, 200));
    }
}
