package dev.blob.tag.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("journal_entry_tag")
public class JournalEntryTagEntity {

    private Long journalEntryId;
    private Long tagId;

    public JournalEntryTagEntity() {
    }

    public JournalEntryTagEntity(Long journalEntryId, Long tagId) {
        this.journalEntryId = journalEntryId;
        this.tagId = tagId;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}
