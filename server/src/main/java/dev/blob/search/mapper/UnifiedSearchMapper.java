package dev.blob.search.mapper;

import dev.blob.search.dto.UnifiedSearchQuery;
import dev.blob.search.dto.UnifiedSearchRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UnifiedSearchMapper {
    long countSearch(@Param("query") UnifiedSearchQuery query);

    List<UnifiedSearchRow> search(@Param("query") UnifiedSearchQuery query,
                                 @Param("offset") long offset, @Param("pageSize") int pageSize);
}
