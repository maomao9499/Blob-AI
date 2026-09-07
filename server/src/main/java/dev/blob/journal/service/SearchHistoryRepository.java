package dev.blob.journal.service;

import java.util.List;

public interface SearchHistoryRepository {

    void record(String keyword);

    List<String> recent(int limit);

    List<String> popular(int limit);
}
