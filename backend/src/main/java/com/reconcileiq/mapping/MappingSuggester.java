package com.reconcileiq.mapping;

import com.reconcileiq.ingestion.CanonicalField;

import java.util.*;

/**
 * Interface for column-mapping suggestions. AI is limited to this one boundary:
 * proposing which partner header maps to which canonical field. It never touches
 * amounts, statuses, freight rates, or claim decisions.
 */
public interface MappingSuggester {

    /**
     * Suggest a mapping from partner headers to canonical fields.
     *
     * @param headers   the raw header row from the partner file
     * @param sampleRows the first N rows of the file (used for data-type sniffing)
     * @return map of header -> suggested canonical field key with confidence and reason
     */
    List<Suggestion> suggest(List<String> headers, List<Map<String, String>> sampleRows);

    class Suggestion {
        public final String header;
        public final String canonicalField;
        public final double confidence;
        public final String reason;

        public Suggestion(String header, String canonicalField, double confidence, String reason) {
            this.header = header;
            this.canonicalField = canonicalField;
            this.confidence = confidence;
            this.reason = reason;
        }
    }
}