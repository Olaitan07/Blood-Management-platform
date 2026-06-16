package com.blood.report.service;

import com.blood.report.dto.ReportResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts a {@link ReportResult} to RFC-4180 CSV.
 * Column headers are derived from the union of all row keys, preserving insertion order.
 */
@Component
public class CsvExportService {

    public String toCsv(ReportResult result) {
        List<Map<String, Object>> rows = result.rows();
        if (rows.isEmpty()) {
            return "# No data for " + result.type() + " (" + result.from() + " – " + result.to() + ")\n" +
                   (result.note() != null ? "# " + result.note() + "\n" : "");
        }

        // Collect headers from all rows (order-preserving)
        Set<String> headers = new LinkedHashSet<>();
        rows.forEach(r -> headers.addAll(r.keySet()));

        StringBuilder sb = new StringBuilder();
        // Report metadata comment lines
        sb.append("# Report: ").append(result.type())
          .append(" | Range: ").append(result.from()).append(" – ").append(result.to()).append('\n');
        if (result.note() != null) sb.append("# Note: ").append(result.note()).append('\n');

        // Header row
        sb.append(String.join(",", headers)).append('\n');

        // Data rows
        for (Map<String, Object> row : rows) {
            List<String> values = new ArrayList<>();
            for (String header : headers) {
                Object val = row.getOrDefault(header, "");
                values.add(escapeCsv(val != null ? val.toString() : ""));
            }
            sb.append(String.join(",", values)).append('\n');
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
