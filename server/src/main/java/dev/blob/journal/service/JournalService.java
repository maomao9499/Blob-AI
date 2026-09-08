package dev.blob.journal.service;

import dev.blob.common.api.PageResponse;
import dev.blob.journal.dto.JournalCreateRequest;
import dev.blob.journal.dto.JournalDetailResponse;
import dev.blob.journal.dto.JournalQuery;
import dev.blob.journal.dto.JournalSummaryResponse;
import dev.blob.journal.dto.JournalUpdateRequest;

public interface JournalService {

    long create(JournalCreateRequest request);

    JournalDetailResponse get(long id);

    /** Reads and locks the current MySQL source within the caller transaction; bypasses Redis. */
    JournalDetailResponse getAuthoritative(long id);

    void update(long id, JournalUpdateRequest request);

    void delete(long id);

    PageResponse<JournalSummaryResponse> search(JournalQuery query);
}
