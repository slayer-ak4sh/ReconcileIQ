package com.reconcileiq.mapping;

import java.util.List;
import java.util.Map;

public class MappingSuggestionResponse {
    public final Long sourceId;
    public final String fileType;
    public final List<MappingSuggester.Suggestion> suggestions;
    public final List<Map<String, String>> previewRows;
    public final boolean allMapped;

    public MappingSuggestionResponse(Long sourceId, String fileType,
                                     List<MappingSuggester.Suggestion> suggestions,
                                     List<Map<String, String>> previewRows) {
        this(sourceId, fileType, suggestions, previewRows, false);
    }

    public MappingSuggestionResponse(Long sourceId, String fileType,
                                     List<MappingSuggester.Suggestion> suggestions,
                                     List<Map<String, String>> previewRows,
                                     boolean allMapped) {
        this.sourceId = sourceId;
        this.fileType = fileType;
        this.suggestions = suggestions;
        this.previewRows = previewRows;
        this.allMapped = allMapped;
    }
}