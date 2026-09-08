package dev.blob.tag.service;

import dev.blob.tag.dto.TagCreateRequest;
import dev.blob.tag.dto.TagResponse;
import dev.blob.tag.dto.TagUpdateRequest;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TagService {

    List<TagResponse> list();

    long create(TagCreateRequest request);

    void update(long id, TagUpdateRequest request);

    void delete(long id);

    void replaceJournalTags(long journalId, List<Long> tagIds);

    void replaceKnowledgeTags(long knowledgeId, List<Long> tagIds);

    Map<Long, List<TagResponse>> findByKnowledgeIds(Collection<Long> knowledgeIds);

    Map<Long, List<TagResponse>> findByJournalIds(Collection<Long> journalIds);
}
