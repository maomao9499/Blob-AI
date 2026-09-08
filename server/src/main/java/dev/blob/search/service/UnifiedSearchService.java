package dev.blob.search.service;

import dev.blob.common.api.PageResponse;
import dev.blob.search.dto.UnifiedSearchQuery;
import dev.blob.search.dto.UnifiedSearchResponse;

public interface UnifiedSearchService {
    PageResponse<UnifiedSearchResponse> search(UnifiedSearchQuery query);
}
