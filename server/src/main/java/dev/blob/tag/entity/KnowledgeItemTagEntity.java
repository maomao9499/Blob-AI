package dev.blob.tag.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("knowledge_item_tag")
public class KnowledgeItemTagEntity {

    private Long knowledgeItemId;
    private Long tagId;

    public KnowledgeItemTagEntity() {
    }

    public KnowledgeItemTagEntity(Long knowledgeItemId, Long tagId) {
        this.knowledgeItemId = knowledgeItemId;
        this.tagId = tagId;
    }

    public Long getKnowledgeItemId() {
        return knowledgeItemId;
    }

    public void setKnowledgeItemId(Long knowledgeItemId) {
        this.knowledgeItemId = knowledgeItemId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}
