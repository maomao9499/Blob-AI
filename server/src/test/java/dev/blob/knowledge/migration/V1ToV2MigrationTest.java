package dev.blob.knowledge.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.net.URI;
import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class V1ToV2MigrationTest {
    @Test void incrementalMigrationPreservesAllV1RowsAndRelativeImageReferences() throws Exception {
        String url = System.getenv().getOrDefault("BLOB_TEST_DB_URL",
                "jdbc:mysql://127.0.0.1:3306/blob_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai");
        String username = System.getenv().getOrDefault("BLOB_TEST_DB_USERNAME","root");
        String password = System.getenv().getOrDefault("BLOB_TEST_DB_PASSWORD","");
        String schema = "blob_m2_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        URI source = URI.create(url.substring("jdbc:".length()));
        String isolatedUrl = "jdbc:mysql://" + source.getRawAuthority() + "/" + schema
                + (source.getRawQuery() == null ? "" : "?" + source.getRawQuery());
        try (var admin = DriverManager.getConnection(url,username,password);
             var statement = admin.createStatement()) {
            statement.executeUpdate("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4");
            try {
                Flyway.configure().dataSource(isolatedUrl,username,password).locations("classpath:db/migration")
                        .cleanDisabled(true).target("1").load().migrate();
                JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(isolatedUrl,username,password));
                jdbc.update("INSERT INTO journal_entry(id,title,content_md,entry_type,entry_date) VALUES (1,?,?,?,?)",
                        "迁移前日志","# 保留原文\n![图片](/api/v1/media/images/upgrade.png)","LEARNING","2026-09-07");
                jdbc.update("INSERT INTO tag(id,name,color) VALUES (1,?,?)","升级标签","#123456");
                jdbc.update("INSERT INTO journal_entry_tag(journal_entry_id,tag_id) VALUES(1,1)");
                jdbc.update("INSERT INTO media_asset(original_name,stored_name,storage_path,mime_type,file_size,sha256) VALUES(?,?,?,?,?,?)",
                        "图片.png","upgrade.png","images/upgrade.png","image/png",123,"a".repeat(64));
                Map<String,Object> journal = jdbc.queryForMap("SELECT * FROM journal_entry WHERE id=1");
                Map<String,Object> tag = jdbc.queryForMap("SELECT * FROM tag WHERE id=1");
                Map<String,Object> media = jdbc.queryForMap("SELECT * FROM media_asset");

                var migration = Flyway.configure().dataSource(isolatedUrl,username,password)
                        .locations("classpath:db/migration").cleanDisabled(true).load().migrate();

                assertThat(migration.migrationsExecuted).isEqualTo(1);
                assertThat(jdbc.queryForMap("SELECT * FROM journal_entry WHERE id=1")).isEqualTo(journal);
                assertThat(jdbc.queryForMap("SELECT * FROM tag WHERE id=1")).isEqualTo(tag);
                assertThat(jdbc.queryForMap("SELECT * FROM media_asset")).isEqualTo(media);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entry_tag WHERE journal_entry_id=1 AND tag_id=1",Integer.class)).isEqualTo(1);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_item",Integer.class)).isZero();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_category",Integer.class)).isZero();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_relation",Integer.class)).isZero();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_item_tag",Integer.class)).isZero();
            } finally {
                // This UUID schema was created above by this test; shared databases are never cleaned or dropped.
                statement.executeUpdate("DROP DATABASE `" + schema + "`");
            }
        }
    }
}
