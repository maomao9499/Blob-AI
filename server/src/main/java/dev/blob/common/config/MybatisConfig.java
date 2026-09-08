package dev.blob.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({"dev.blob.journal.mapper", "dev.blob.tag.mapper", "dev.blob.media.mapper",
        "dev.blob.knowledge.mapper", "dev.blob.search.mapper"})
public class MybatisConfig {
}
