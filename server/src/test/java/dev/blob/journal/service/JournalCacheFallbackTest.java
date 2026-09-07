package dev.blob.journal.service;

import dev.blob.journal.dto.JournalUpdateRequest;
import dev.blob.journal.entity.JournalEntryEntity;
import dev.blob.journal.entity.JournalEntryEntity.EntryType;
import dev.blob.journal.mapper.JournalEntryMapper;
import dev.blob.tag.service.TagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalCacheFallbackTest {

    @Mock
    private JournalEntryMapper journalMapper;

    @Mock
    private TagService tagService;

    @Mock
    private JournalCacheRepository cache;

    @Mock
    private SearchHistoryRepository searchHistory;

    @InjectMocks
    private JournalServiceImpl service;

    @Test
    void redisReadFailureFallsBackToMysql() {
        when(cache.get(9L)).thenThrow(new RedisConnectionFailureException("down"));
        when(journalMapper.selectById(9L)).thenReturn(fixture(9L));
        when(tagService.findByJournalIds(List.of(9L))).thenReturn(Map.of(9L, List.of()));

        assertThat(service.get(9L).id()).isEqualTo(9L);

        verify(journalMapper).selectById(9L);
    }

    @Test
    void cacheWriteFailureDoesNotFailMysqlRead() {
        when(cache.get(9L)).thenReturn(java.util.Optional.empty());
        when(journalMapper.selectById(9L)).thenReturn(fixture(9L));
        when(tagService.findByJournalIds(List.of(9L))).thenReturn(Map.of(9L, List.of()));
        doThrow(new RedisConnectionFailureException("down")).when(cache).put(any());

        assertThat(service.get(9L).title()).isEqualTo("Redis");
    }

    @Test
    void updateAndDeleteInvalidateCacheAfterMysqlMutation() {
        JournalEntryEntity entry = fixture(9L);
        when(journalMapper.selectById(9L)).thenReturn(entry);

        service.update(9L, new JournalUpdateRequest(
                "更新", "正文", "LEARNING", LocalDate.of(2026, 9, 7), List.of()
        ));
        service.delete(9L);

        verify(cache, org.mockito.Mockito.times(2)).evict(9L);
    }

    @Test
    void searchRecordsNormalizedNonBlankKeyword() {
        when(journalMapper.countSearch(any())).thenReturn(0L);

        service.search(new dev.blob.journal.dto.JournalQuery(
                "  Redis  ", null, null, null, null, 1, 20
        ));

        verify(searchHistory).record("Redis");
    }

    private JournalEntryEntity fixture(long id) {
        JournalEntryEntity entry = JournalEntryEntity.create(
                "Redis", "正文", EntryType.LEARNING, LocalDate.of(2026, 9, 7)
        );
        entry.setId(id);
        return entry;
    }
}
