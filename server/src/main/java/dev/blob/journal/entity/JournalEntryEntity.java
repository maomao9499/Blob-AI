package dev.blob.journal.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("journal_entry")
public class JournalEntryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String contentMd;
    private EntryType entryType;
    private LocalDate entryDate;
    private String aiSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static JournalEntryEntity create(
            String title,
            String contentMd,
            EntryType entryType,
            LocalDate entryDate
    ) {
        JournalEntryEntity entry = new JournalEntryEntity();
        entry.setTitle(title);
        entry.setContentMd(contentMd);
        entry.setEntryType(entryType);
        entry.setEntryDate(entryDate);
        return entry;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentMd() {
        return contentMd;
    }

    public void setContentMd(String contentMd) {
        this.contentMd = contentMd;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum EntryType {
        LEARNING("LEARNING"),
        LIFE("LIFE");

        @EnumValue
        private final String value;

        EntryType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
