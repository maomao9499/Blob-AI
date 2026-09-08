package dev.blob.knowledge.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.blob.knowledge.entity.KnowledgeRelationEntity;
import dev.blob.knowledge.dto.KnowledgeRelationResponse;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface KnowledgeRelationMapper extends BaseMapper<KnowledgeRelationEntity> {
    List<KnowledgeRelationResponse> findRelations(@Param("id") long id,
            @Param("offset") long offset, @Param("pageSize") int pageSize);
}
