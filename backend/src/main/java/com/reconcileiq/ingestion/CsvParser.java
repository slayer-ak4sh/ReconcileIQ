package com.reconcileiq.ingestion;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class CsvParser {

    public ParseResult parse(MultipartFile file, int previewLimit, Consumer<Map<String, String>> rowConsumer) throws Exception {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> previewRows = new ArrayList<>();
        int rowCount = 0;
        int validCount = 0;
        int errorCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build())) {

            headers.addAll(csvParser.getHeaderNames());

            for (CSVRecord record : csvParser) {
                rowCount++;
                Map<String, String> rowMap = record.toMap();

                if (rowMap.values().stream().allMatch(v -> v == null || v.trim().isEmpty())) {
                    errorCount++;
                    continue;
                }

                validCount++;
                rowConsumer.accept(rowMap);

                if (previewRows.size() < previewLimit) {
                    previewRows.add(rowMap);
                }
            }
        }

        return new ParseResult(headers, previewRows, rowCount, validCount, errorCount);
    }

    public static class ParseResult {
        public final List<String> headers;
        public final List<Map<String, String>> previewRows;
        public final int totalRows;
        public final int validRows;
        public final int errorRows;

        public ParseResult(List<String> headers, List<Map<String, String>> previewRows, int totalRows, int validRows, int errorRows) {
            this.headers = headers;
            this.previewRows = previewRows;
            this.totalRows = totalRows;
            this.validRows = validRows;
            this.errorRows = errorRows;
        }
    }
}