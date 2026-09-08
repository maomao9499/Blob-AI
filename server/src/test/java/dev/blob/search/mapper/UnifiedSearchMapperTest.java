package dev.blob.search.mapper;

import dev.blob.search.dto.UnifiedSearchQuery;
import dev.blob.search.dto.UnifiedSearchRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UnifiedSearchMapperTest {
    @Autowired private UnifiedSearchMapper mapper;
    @Autowired private JdbcTemplate jdbc;
    private long nextId;
    private long fixtureTag;
    private final LocalDateTime time = LocalDateTime.of(2026, 9, 8, 12, 0);

    @BeforeEach
    void createIsolatedFixtureTagAndIds() {
        nextId = jdbc.queryForObject("SELECT GREATEST(COALESCE((SELECT MAX(id) FROM journal_entry),0), "
                + "COALESCE((SELECT MAX(id) FROM knowledge_item),0)) + 100", Long.class);
        fixtureTag = insertTag("search-" + UUID.randomUUID());
    }

    @Test
    void globallyPaginatesFortyFiveMixedRowsWithoutDuplicatesOrMissingRows() {
        var expected = new ArrayList<UnifiedSearchRow>();
        for (int index = 0; index < 45; index++) {
            String type = index % 2 == 0 ? "JOURNAL" : "KNOWLEDGE";
            long id = nextId++;
            LocalDateTime updated = time.plusSeconds(index / 3);
            insert(type, id, "分页内容 " + index, "正文", updated);
            expected.add(new UnifiedSearchRow(type, id, "分页内容 " + index, "正文", updated));
        }
        Comparator<UnifiedSearchRow> order = Comparator.comparing(UnifiedSearchRow::updatedAt).reversed()
                .thenComparing(UnifiedSearchRow::sourceType)
                .thenComparing(UnifiedSearchRow::sourceId, Comparator.reverseOrder());
        expected.sort(order);
        var actual = new ArrayList<UnifiedSearchRow>();
        for (int page = 1; page <= 3; page++) {
            var query = query("  ", "ALL", page);
            assertThat(mapper.countSearch(query)).isEqualTo(45);
            var rows = mapper.search(query, (page - 1) * 20L, 20);
            assertThat(rows).hasSize(page == 3 ? 5 : 20);
            assertThat(mapper.search(query, (page - 1) * 20L, 20)).isEqualTo(rows);
            actual.addAll(rows);
        }
        assertThat(actual).containsExactlyElementsOf(expected);
        assertThat(actual.stream().map(row -> row.sourceType() + row.sourceId())).doesNotHaveDuplicates();
    }

    @Test
    void keepsIdenticalNumericIdsAndMatchesUnicodeTitleBodyAndTagWithoutDuplicates() {
        long sameId = nextId++;
        insert("JOURNAL", sameId, "中文关键词标题", "普通正文", time);
        insert("KNOWLEDGE", sameId, "知识标题", "中文关键词正文", time);
        long tagOnly = nextId++;
        insert("KNOWLEDGE", tagOnly, "普通标题", "普通正文", time);
        for (String suffix : List.of("甲", "乙")) {
            long tag = insertTag("中文关键词" + suffix + UUID.randomUUID());
            attach("KNOWLEDGE", tagOnly, tag);
        }
        assertRows("中文关键词", "ALL", List.of("JOURNAL:" + sameId, "KNOWLEDGE:" + tagOnly, "KNOWLEDGE:" + sameId));
        assertRows("中文关键词", "JOURNAL", List.of("JOURNAL:" + sameId));
        assertRows("中文关键词", "KNOWLEDGE", List.of("KNOWLEDGE:" + tagOnly, "KNOWLEDGE:" + sameId));
        assertRows(null, "ALL", List.of("JOURNAL:" + sameId, "KNOWLEDGE:" + tagOnly, "KNOWLEDGE:" + sameId));
        assertRows("   ", "JOURNAL", List.of("JOURNAL:" + sameId));
        var untaggedQuery = new UnifiedSearchQuery("中文关键词", "ALL", null, 1, 100);
        assertThat(mapper.countSearch(untaggedQuery)).isEqualTo(3);
        assertThat(mapper.search(untaggedQuery, 0, 100)).hasSize(3);
        assertThat(mapper.countSearch(new UnifiedSearchQuery(null, "ALL", Long.MAX_VALUE, 1, 20))).isZero();
    }

    @Test
    void treatsWildcardsEscapeBackslashAndSqlInjectionAsLiteralText() {
        long percent = nextId++;
        long underscore = nextId++;
        long escape = nextId++;
        long slash = nextId++;
        long injection = nextId++;
        insert("JOURNAL", percent, "完成100%", "普通正文", time);
        insert("KNOWLEDGE", underscore, "snake_case", "普通正文", time);
        insert("JOURNAL", escape, "hello!world", "普通正文", time);
        insert("KNOWLEDGE", slash, "普通标题", "C:\\notes", time);
        insert("KNOWLEDGE", injection, "安全内容", "' OR 1=1 --", time);
        insert("JOURNAL", nextId++, "完全无关", "正文", time);
        assertRows("%", "ALL", List.of("JOURNAL:" + percent));
        assertRows("_", "ALL", List.of("KNOWLEDGE:" + underscore));
        assertRows("!", "ALL", List.of("JOURNAL:" + escape));
        assertRows("\\", "ALL", List.of("KNOWLEDGE:" + slash));
        assertRows("' OR 1=1 --", "ALL", List.of("KNOWLEDGE:" + injection));
        assertRows("' UNION SELECT 1 --", "ALL", List.of());
        long literalTag = insertTag("literal%_!" + UUID.randomUUID());
        attach("KNOWLEDGE", underscore, literalTag);
        assertRows("literal%_!", "ALL", List.of("KNOWLEDGE:" + underscore));
    }

    private UnifiedSearchQuery query(String keyword, String type, int page) {
        return new UnifiedSearchQuery(keyword, type, fixtureTag, page, 20);
    }

    private void assertRows(String keyword, String type, List<String> expected) {
        var query = query(keyword, type, 1);
        assertThat(mapper.countSearch(query)).isEqualTo(expected.size());
        assertThat(mapper.search(query, 0, 20)).extracting(row -> row.sourceType() + ":" + row.sourceId())
                .containsExactlyElementsOf(expected);
    }

    private void insert(String type, long id, String title, String body, LocalDateTime updated) {
        if (type.equals("JOURNAL")) {
            jdbc.update("INSERT INTO journal_entry (id,title,content_md,entry_type,entry_date,updated_at) "
                    + "VALUES (?,?,?,'LEARNING','2026-09-08',?)", id, title, body, updated);
        } else {
            jdbc.update("INSERT INTO knowledge_item (id,title,content_md,updated_at) VALUES (?,?,?,?)",
                    id, title, body, updated);
        }
        attach(type, id, fixtureTag);
    }

    private long insertTag(String name) {
        jdbc.update("INSERT INTO tag (name) VALUES (?)", name);
        return jdbc.queryForObject("SELECT id FROM tag WHERE name = ?", Long.class, name);
    }

    private void attach(String type, long id, long tag) {
        if (type.equals("JOURNAL")) {
            jdbc.update("INSERT INTO journal_entry_tag (journal_entry_id,tag_id) VALUES (?,?)", id, tag);
        } else {
            jdbc.update("INSERT INTO knowledge_item_tag (knowledge_item_id,tag_id) VALUES (?,?)", id, tag);
        }
    }
}
