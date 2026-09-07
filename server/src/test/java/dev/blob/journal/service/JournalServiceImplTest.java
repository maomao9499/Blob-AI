package dev.blob.journal.service;

import dev.blob.common.error.BusinessException;
import dev.blob.common.error.ErrorCode;
import dev.blob.journal.dto.JournalCreateRequest;
import dev.blob.journal.dto.JournalQuery;
import dev.blob.journal.dto.JournalUpdateRequest;
import dev.blob.journal.entity.JournalEntryEntity;
import dev.blob.journal.entity.JournalEntryEntity.EntryType;
import dev.blob.journal.mapper.JournalEntryMapper;
import dev.blob.tag.service.TagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalServiceImplTest {

    @Mock
    private JournalEntryMapper journalMapper;

    @Mock
    private TagService tagService;

    @Mock
    private JournalCacheRepository cacheRepository;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @InjectMocks
    private JournalServiceImpl service;

    @Test
    void createNormalizesTitleAndReturnsGeneratedId() {
        doAnswer(invocation -> {
            ((JournalEntryEntity) invocation.getArgument(0)).setId(42L);
            return 1;
        }).when(journalMapper).insert(any(JournalEntryEntity.class));

        long id = service.create(new JournalCreateRequest(
                "  Redis 学习  ",
                "正文",
                "LEARNING",
                LocalDate.of(2026, 9, 7),
                List.of()
        ));

        ArgumentCaptor<JournalEntryEntity> captor = ArgumentCaptor.forClass(JournalEntryEntity.class);
        verify(journalMapper).insert(captor.capture());
        assertThat(id).isEqualTo(42L);
        assertThat(captor.getValue().getTitle()).isEqualTo("Redis 学习");
        assertThat(captor.getValue().getEntryType()).isEqualTo(EntryType.LEARNING);
    }

    @Test
    void updateUnknownJournalThrowsNotFound() {
        when(journalMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.update(99L, new JournalUpdateRequest(
                "标题", "正文", "LIFE", LocalDate.of(2026, 9, 7), List.of()
        )))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

        verify(journalMapper, never()).updateById(any(JournalEntryEntity.class));
    }

    @Test
    void deleteUnknownJournalThrowsNotFound() {
        when(journalMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

        verify(journalMapper, never()).deleteById(99L);
    }

    @Test
    void searchCapsPageSizeAndCalculatesOffset() {
        JournalEntryEntity newest = JournalEntryEntity.create(
                "第二篇", "内容二", EntryType.LEARNING, LocalDate.of(2026, 9, 7)
        );
        newest.setId(2L);
        JournalEntryEntity older = JournalEntryEntity.create(
                "第一篇", "内容一", EntryType.LIFE, LocalDate.of(2026, 9, 6)
        );
        older.setId(1L);
        when(journalMapper.search(any(JournalQuery.class), anyLong(), anyInt()))
                .thenReturn(List.of(newest, older));
        when(journalMapper.countSearch(any(JournalQuery.class))).thenReturn(102L);

        var page = service.search(new JournalQuery(null, null, null, null, null, 2, 200));

        assertThat(page.page()).isEqualTo(2);
        assertThat(page.pageSize()).isEqualTo(100);
        assertThat(page.total()).isEqualTo(102);
        assertThat(page.items()).extracting("id").containsExactly(2L, 1L);
        verify(journalMapper).search(any(), org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.eq(100));
    }

    @Test
    void queryRejectsPageBelowOne() {
        assertThatThrownBy(() -> new JournalQuery(null, null, null, null, null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }
}
