package dev.blob.journal.service;

import dev.blob.journal.dto.JournalDetailResponse;

import java.util.Optional;

public interface JournalCacheRepository {

    Optional<JournalDetailResponse> get(long id);

    void put(JournalDetailResponse journal);

    void evict(long id);
}
