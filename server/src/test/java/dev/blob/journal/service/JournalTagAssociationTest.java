package dev.blob.journal.service;

import dev.blob.journal.dto.JournalCreateRequest;
import dev.blob.journal.entity.JournalEntryEntity;
import dev.blob.journal.mapper.JournalEntryMapper;
import dev.blob.tag.service.TagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JournalTagAssociationTest {

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
    void createReplacesTagsAfterJournalGetsGeneratedId() {
        doAnswer(invocation -> {
            ((JournalEntryEntity) invocation.getArgument(0)).setId(42L);
            return 1;
        }).when(journalMapper).insert(any(JournalEntryEntity.class));

        service.create(new JournalCreateRequest(
                "MySQL 学习", "正文", "LEARNING", LocalDate.of(2026, 9, 7), List.of(3L, 8L)
        ));

        verify(tagService).replaceJournalTags(42L, List.of(3L, 8L));
    }
}
