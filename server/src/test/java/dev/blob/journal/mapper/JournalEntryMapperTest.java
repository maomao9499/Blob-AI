package dev.blob.journal.mapper;

import dev.blob.journal.entity.JournalEntryEntity;
import dev.blob.journal.entity.JournalEntryEntity.EntryType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JournalEntryMapperTest {

    @Autowired
    private JournalEntryMapper mapper;

    @Test
    void insertsAndReadsUtf8Markdown() {
        JournalEntryEntity entry = JournalEntryEntity.create(
                "Redis 学习",
                "# 缓存\n今天学习 Cache Aside",
                EntryType.LEARNING,
                LocalDate.of(2026, 9, 7)
        );

        mapper.insert(entry);

        assertThat(mapper.selectById(entry.getId()).getContentMd()).contains("今天学习");
    }
}
