package com.blood.search.service;

import com.blood.hospital.dto.HospitalResponse;
import com.blood.hospital.service.HospitalService;
import com.blood.inventory.search.AvailableBloodDto;
import com.blood.inventory.search.BloodSearchPort;
import com.blood.search.dto.BloodSearchResponse;
import com.blood.search.dto.BloodSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class BloodSearchServiceImpl implements BloodSearchService {

    private static final Map<String, List<String>> COMPATIBLE_DONORS = Map.of(
            "O-",  List.of("O-"),
            "O+",  List.of("O+", "O-"),
            "A-",  List.of("A-", "O-"),
            "A+",  List.of("A+", "A-", "O+", "O-"),
            "B-",  List.of("B-", "O-"),
            "B+",  List.of("B+", "B-", "O+", "O-"),
            "AB-", List.of("AB-", "A-", "B-", "O-"),
            "AB+", List.of("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-")
    );

    private final HospitalService hospitalService;
    private final BloodSearchPort bloodSearchPort;
    private final Clock clock;

    @Override
    public BloodSearchResponse search(String bloodGroup, int page, int size, Authentication auth) {
        LocalDate today = LocalDate.now(clock);

        List<HospitalResponse> activeHospitals = hospitalService.listActiveHospitals();
        List<Long> activeIds = activeHospitals.stream().map(HospitalResponse::id).toList();
        Map<Long, HospitalResponse> hospitalById = activeHospitals.stream()
                .collect(Collectors.toMap(HospitalResponse::id, Function.identity()));

        List<AvailableBloodDto> available = bloodSearchPort.findAvailable(bloodGroup, activeIds, today);

        if (available.isEmpty()) {
            List<String> suggestions = buildSuggestions(bloodGroup);
            return new BloodSearchResponse(bloodGroup, page, size, 0, 0, List.of(), suggestions);
        }

        Long requesterHospitalId = extractHospitalId(auth);
        String requesterState = requesterHospitalId != null
                ? hospitalById.getOrDefault(requesterHospitalId, dummyHospital()).state()
                : null;
        String requesterCity = requesterHospitalId != null
                ? hospitalById.getOrDefault(requesterHospitalId, dummyHospital()).city()
                : null;

        List<BloodSearchResult> enriched = available.stream()
                .filter(dto -> hospitalById.containsKey(dto.hospitalId()))
                .map(dto -> {
                    HospitalResponse h = hospitalById.get(dto.hospitalId());
                    return new BloodSearchResult(
                            h.id(),
                            h.name(),
                            h.city(),
                            h.state(),
                            dto.bloodGroup(),
                            dto.availableUnits(),
                            dto.lastUpdated() != null ? dto.lastUpdated().toString() : null
                    );
                })
                .sorted(proximityComparator(requesterState, requesterCity))
                .toList();

        long total = enriched.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int fromIndex = Math.min(page * size, enriched.size());
        int toIndex = Math.min(fromIndex + size, enriched.size());
        List<BloodSearchResult> pageContent = enriched.subList(fromIndex, toIndex);

        return new BloodSearchResponse(bloodGroup, page, size, total, totalPages, pageContent, null);
    }

    private Comparator<BloodSearchResult> proximityComparator(String requesterState, String requesterCity) {
        return Comparator
                .<BloodSearchResult, Integer>comparing(r -> proximityScore(r, requesterState, requesterCity))
                .thenComparing(Comparator.comparingInt(BloodSearchResult::availableUnits).reversed());
    }

    /** Lower score = closer. 0 = same city+state, 1 = same state, 2 = other */
    private int proximityScore(BloodSearchResult result, String state, String city) {
        if (state == null) return 2;
        if (state.equalsIgnoreCase(result.state()) && city != null && city.equalsIgnoreCase(result.city())) return 0;
        if (state.equalsIgnoreCase(result.state())) return 1;
        return 2;
    }

    private List<String> buildSuggestions(String requestedGroup) {
        List<String> compatible = COMPATIBLE_DONORS.getOrDefault(requestedGroup, List.of());
        return compatible.stream()
                .filter(g -> !g.equals(requestedGroup))
                .toList();
    }

    private Long extractHospitalId(Authentication auth) {
        if (auth == null || auth.getDetails() == null) return null;
        if (auth.getDetails() instanceof Map<?, ?> details) {
            Object id = details.get("hospitalId");
            if (id instanceof Long l) return l;
            if (id instanceof Number n) return n.longValue();
        }
        return null;
    }

    private HospitalResponse dummyHospital() {
        return new HospitalResponse(null, null, null, null, null, null, null, null);
    }
}
