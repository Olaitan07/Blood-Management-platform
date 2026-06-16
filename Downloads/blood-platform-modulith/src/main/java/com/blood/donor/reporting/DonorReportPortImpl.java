package com.blood.donor.reporting;

import com.blood.donor.model.Donor;
import com.blood.donor.repository.DonorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class DonorReportPortImpl implements DonorReportPort {

    private final DonorRepository donorRepository;

    @Override
    @Transactional(readOnly = true)
    public DonorStatDto stats(LocalDate from, LocalDate to) {
        List<Donor> all = donorRepository.findAll();

        Map<String, Long> byBloodGroup = all.stream()
                .collect(Collectors.groupingBy(d -> d.getBloodGroup().name(), Collectors.counting()));

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();
        long inRange = all.stream()
                .filter(d -> d.getCreatedAt() != null
                        && !d.getCreatedAt().isBefore(fromDt)
                        && d.getCreatedAt().isBefore(toDt))
                .count();

        String note = inRange == 0 ? "No donor registrations in the selected date range." : null;
        return new DonorStatDto(all.size(), byBloodGroup, inRange, note);
    }
}
