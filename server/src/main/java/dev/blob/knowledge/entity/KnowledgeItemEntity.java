package dev.blob.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("knowledge_item")
public class KnowledgeItemEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String contentMd;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String summary;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long categoryId;
    private Long sourceJournalId;
    private String sourceJournalTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContentMd() { return contentMd; }
    public void setContentMd(String contentMd) { this.contentMd = contentMd; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getSourceJournalId() { return sourceJournalId; }
    public void setSourceJournalId(Long sourceJournalId) { this.sourceJournalId = sourceJournalId; }
    public String getSourceJournalTitle() { return sourceJournalTitle; }
    public void setSourceJournalTitle(String sourceJournalTitle) { this.sourceJournalTitle = sourceJournalTitle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
