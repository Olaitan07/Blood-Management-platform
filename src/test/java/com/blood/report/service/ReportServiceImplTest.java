package com.blood.report.service;

import com.blood.report.dto.ReportResult;
import com.blood.report.model.ReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceImplTest {

    private CsvExportService csvExportService;
    private ReportGenerator fakeGenerator;
    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        csvExportService = mock(CsvExportService.class);
        fakeGenerator = mock(ReportGenerator.class);
        when(fakeGenerator.type()).thenReturn(ReportType.STOCK_LEVELS);
        service = new ReportServiceImpl(List.of(fakeGenerator), csvExportService);
    }

    @Test
    void generate_fromAfterTo_throwsWithoutCallingGenerator() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> service.generate(ReportType.STOCK_LEVELS, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before");
        verify(fakeGenerator, never()).generate(any(), any());
    }

    @Test
    void generate_exactlyMaxRangeDays_isAllowed() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = from.plusDays(365); // exactly 365 days between — the documented ceiling, inclusive
        ReportResult stub = new ReportResult(ReportType.STOCK_LEVELS, from, to, 0, List.of(), null);
        when(fakeGenerator.generate(from, to)).thenReturn(stub);

        ReportResult result = service.generate(ReportType.STOCK_LEVELS, from, to);

        assertThat(result).isEqualTo(stub);
    }

    @Test
    void generate_rangeOfThreeHundredSixtySixDays_throws() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = from.plusDays(366); // one day past the ceiling

        assertThatThrownBy(() -> service.generate(ReportType.STOCK_LEVELS, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("365");
        verify(fakeGenerator, never()).generate(any(), any());
    }

    @Test
    void generate_unknownReportType_throwsIllegalArgument() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        // fakeGenerator only declares STOCK_LEVELS — TRANSFERS has no registered generator here.
        assertThatThrownBy(() -> service.generate(ReportType.TRANSFERS, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TRANSFERS");
    }

    @Test
    void exportCsv_delegatesToGeneratorThenCsvExportService() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        ReportResult stub = new ReportResult(ReportType.STOCK_LEVELS, from, to, 0, List.of(), null);
        when(fakeGenerator.generate(from, to)).thenReturn(stub);
        when(csvExportService.toCsv(stub)).thenReturn("csv,content");

        String csv = service.exportCsv(ReportType.STOCK_LEVELS, from, to);

        assertThat(csv).isEqualTo("csv,content");
    }
}
