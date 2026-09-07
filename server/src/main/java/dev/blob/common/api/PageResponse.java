package dev.blob.common.api;

import java.util.List;

public record PageResponse<T>(List<T> items, long total, int page, int pageSize) {

    public PageResponse {
        items = List.copyOf(items);
    }
}
