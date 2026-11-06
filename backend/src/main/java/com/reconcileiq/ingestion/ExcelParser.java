package com.reconcileiq.ingestion;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.dhatim.fastexcel.reader.Cell;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class ExcelParser {

    public ParseResult parse(MultipartFile file, int previewLimit, Consumer<Map<String, String>> rowConsumer) throws Exception {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> previewRows = new ArrayList<>();
        int rowCount = 0;
        int validCount = 0;
        int errorCount = 0;

        try (InputStream is = file.getInputStream();
             ReadableWorkbook wb = new ReadableWorkbook(is)) {

            Sheet sheet = wb.getFirstSheet();
            if (sheet == null) {
                return new ParseResult(headers, previewRows, 0, 0, 0);
            }

            // Read header row
            Iterator<Row> rowIterator = sheet.openStream().iterator();
            if (!rowIterator.hasNext()) {
                return new ParseResult(headers, previewRows, 0, 0, 0);
            }

            Row headerRow = rowIterator.next();
            for (Cell cell : headerRow) {
                headers.add(cell.getRawValue() != null ? cell.getRawValue().toString().trim() : "");
            }

            // Read data rows
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                rowCount++;

                Map<String, String> rowMap = new HashMap<>();
                boolean hasData = false;

                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i);
                    String value = "";
                    if (cell != null) {
                        Object raw = cell.getRawValue();
                        if (raw != null) {
                            value = raw.toString().trim();
                        }
                    }
                    rowMap.put(headers.get(i), value);
                    if (!value.isEmpty()) hasData = true;
                }

                if (!hasData) {
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