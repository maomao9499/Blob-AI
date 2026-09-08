package dev.blob.search.service;

import dev.blob.journal.service.SearchHistoryRepository;
import dev.blob.search.dto.UnifiedSearchQuery;
import dev.blob.search.dto.UnifiedSearchRow;
import dev.blob.search.mapper.UnifiedSearchMapper;
import dev.blob.tag.dto.TagResponse;
import dev.blob.tag.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.*;

class UnifiedSearchServiceTest {
    private final UnifiedSearchMapper mapper = mock(UnifiedSearchMapper.class);
    private final TagService tags = mock(TagService.class);
    private final SearchHistoryRepository history = mock(SearchHistoryRepository.class);
    private final UnifiedSearchService service = new UnifiedSearchServiceImpl(mapper, tags, history);

    @Test
    void normalizesKeywordAndEscapesLiteralLikeCharacters() {
        var query = new UnifiedSearchQuery("  100%_!\\中文  ", null, null, null, null);
        assertThat(query.keyword()).isEqualTo("100%_!\\中文");
        assertThat(query.likePattern()).isEqualTo("%100!%!_!!\\中文%");
        assertThat(query.sourceType()).isEqualTo("ALL");
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.pageSize()).isEqualTo(20);
        assertThat(new UnifiedSearchQuery(" \t ", "ALL", null, 1, 20).keyword()).isNull();
    }

    @Test
    void rejectsInvalidFiltersAndPagination() {
        assertThatIllegalArgumentException().isThrownBy(() -> new UnifiedSearchQuery(null, "OTHER", null, 1, 20));
        assertThatIllegalArgumentException().isThrownBy(() -> new UnifiedSearchQuery(null, "ALL", 0L, 1, 20));
        assertThatIllegalArgumentException().isThrownBy(() -> new UnifiedSearchQuery(null, "ALL", null, 0, 20));
        assertThatIllegalArgumentException().isThrownBy(() -> new UnifiedSearchQuery(null, "ALL", null, 1, 101));
    }

    @Test
    void separatesSameIdTagsAndSurvivesUnavailableSearchHistory() {
        var query = new UnifiedSearchQuery("  中文  ", "ALL", null, 2, 20);
        var time = LocalDateTime.of(2026, 9, 8, 12, 0);
        when(mapper.countSearch(query)).thenReturn(22L);
        when(mapper.search(query, 20L, 20)).thenReturn(List.of(
                new UnifiedSearchRow("JOURNAL", 7L, "日志", " 第一行\n 第二行 ", time),
                new UnifiedSearchRow("KNOWLEDGE", 7L, "知识", "😀".repeat(201), time)));
        var journalTags = List.of(new TagResponse(1L, "日志标签", null));
        var knowledgeTags = List.of(new TagResponse(2L, "知识标签", "#123456"));
        when(tags.findByJournalIds(List.of(7L))).thenReturn(Map.of(7L, journalTags));
        when(tags.findByKnowledgeIds(List.of(7L))).thenReturn(Map.of(7L, knowledgeTags));
        doThrow(new DataAccessResourceFailureException("Redis unavailable")).when(history).record("中文");

        var result = service.search(query);

        assertThat(result.total()).isEqualTo(22);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).tags()).isEqualTo(journalTags);
        assertThat(result.items().get(1).tags()).isEqualTo(knowledgeTags);
        assertThat(result.items().get(0).excerpt()).isEqualTo("第一行 第二行");
        assertThat(result.items().get(1).excerpt()).isEqualTo("😀".repeat(200));
        verify(tags, times(1)).findByJournalIds(List.of(7L));
        verify(tags, times(1)).findByKnowledgeIds(List.of(7L));
    }

    @Test
    void emptyResultsSkipTagAndPageReadsAndBlankHistory() {
        var query = new UnifiedSearchQuery("  ", "ALL", null, 1, 20);
        when(mapper.countSearch(query)).thenReturn(0L);
        assertThat(service.search(query).items()).isEmpty();
        verify(mapper, never()).search(any(), anyLong(), anyInt());
        verifyNoInteractions(tags, history);
    }
}
