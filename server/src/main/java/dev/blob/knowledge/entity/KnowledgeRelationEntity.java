package dev.blob.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("knowledge_relation")
public class KnowledgeRelationEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sourceKnowledgeId;
    private Long targetKnowledgeId;
    private String relationType;
    private LocalDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSourceKnowledgeId() { return sourceKnowledgeId; }
    public void setSourceKnowledgeId(Long sourceKnowledgeId) { this.sourceKnowledgeId = sourceKnowledgeId; }
    public Long getTargetKnowledgeId() { return targetKnowledgeId; }
    public void setTargetKnowledgeId(Long targetKnowledgeId) { this.targetKnowledgeId = targetKnowledgeId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
