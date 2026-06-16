package com.blood.transfer.reporting;

import com.blood.transfer.model.BloodTransferRequest;
import com.blood.transfer.model.TransferStatus;
import com.blood.transfer.repository.BloodTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class TransferReportPortImpl implements TransferReportPort {

    private final BloodTransferRepository transferRepository;

    @Override
    @Transactional(readOnly = true)
    public TransferSummaryDto summarise(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        List<BloodTransferRequest> transfers =
                transferRepository.findByRequestDateBetween(fromDt, toDt);

        if (transfers.isEmpty()) {
            return new TransferSummaryDto(0, emptyStatusMap(), null,
                    "No transfer data found for the selected date range.");
        }

        Map<String, Long> byStatus = transfers.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));

        // Avg approval time: elapsed minutes from requestDate → approvalDate for APPROVED+ transfers
        Double avgApproval = transfers.stream()
                .filter(t -> t.getApprovalDate() != null)
                .mapToLong(t -> ChronoUnit.MINUTES.between(t.getRequestDate(), t.getApprovalDate()))
                .average()
                .stream().boxed().findFirst().orElse(null);

        return new TransferSummaryDto(transfers.size(), byStatus, avgApproval, null);
    }

    private Map<String, Long> emptyStatusMap() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (TransferStatus s : TransferStatus.values()) m.put(s.name(), 0L);
        return m;
    }
}
