package dev.blob.journal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.blob.journal.dto.JournalQuery;
import dev.blob.journal.entity.JournalEntryEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface JournalEntryMapper extends BaseMapper<JournalEntryEntity> {

    List<JournalEntryEntity> search(
            @Param("query") JournalQuery query,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize
    );

    long countSearch(@Param("query") JournalQuery query);
}
