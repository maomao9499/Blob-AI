package dev.blob.journal.mapper;

import dev.blob.journal.dto.JournalQuery;
import dev.blob.journal.entity.JournalEntryEntity;
import dev.blob.journal.entity.JournalEntryEntity.EntryType;
import dev.blob.tag.entity.JournalEntryTagEntity;
import dev.blob.tag.entity.TagEntity;
import dev.blob.tag.mapper.JournalEntryTagMapper;
import dev.blob.tag.mapper.TagMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JournalSearchMapperTest {

    @Autowired
    private JournalEntryMapper journalMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private JournalEntryTagMapper journalEntryTagMapper;

    @Test
    void combinesKeywordTagTypeAndDateFiltersInTimelineOrder() {
        JournalEntryEntity older = insertJournal(
                "Redis 基础", "缓存入门", EntryType.LEARNING, LocalDate.of(2026, 9, 6)
        );
        JournalEntryEntity newer = insertJournal(
                "Redis 实战", "缓存与持久化", EntryType.LEARNING, LocalDate.of(2026, 9, 7)
        );
        insertJournal("周末散步", "生活随笔", EntryType.LIFE, LocalDate.of(2026, 9, 7));

        TagEntity tag = new TagEntity();
        tag.setName("Redis-search-fixture");
        tagMapper.insert(tag);
        journalEntryTagMapper.insert(List.of(
                new JournalEntryTagEntity(older.getId(), tag.getId()),
                new JournalEntryTagEntity(newer.getId(), tag.getId())
        ));

        JournalQuery query = new JournalQuery(
                "缓存", "LEARNING", tag.getId(), LocalDate.of(2026, 9, 6), LocalDate.of(2026, 9, 7), 1, 20
        );

        assertThat(journalMapper.countSearch(query)).isEqualTo(2);
        assertThat(journalMapper.search(query, 0, 20))
                .extracting(JournalEntryEntity::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    private JournalEntryEntity insertJournal(
            String title,
            String content,
            EntryType type,
            LocalDate date
    ) {
        JournalEntryEntity entry = JournalEntryEntity.create(title, content, type, date);
        journalMapper.insert(entry);
        return entry;
    }
}
