package dev.blob.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.blob.knowledge.entity.KnowledgeItemEntity;
import dev.blob.knowledge.dto.KnowledgeQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface KnowledgeItemMapper extends BaseMapper<KnowledgeItemEntity> {
    long countSearch(@Param("query") KnowledgeQuery query);
    List<KnowledgeItemEntity> search(@Param("query") KnowledgeQuery query,
            @Param("offset") long offset, @Param("pageSize") int pageSize);
}
