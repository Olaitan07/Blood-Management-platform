package com.blood.search.controller;

import com.blood.inventory.search.BloodSearchPort;
import com.blood.search.dto.ApiResponse;
import com.blood.search.dto.BloodSearchResponse;
import com.blood.search.service.BloodSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class BloodSearchController {

    private final BloodSearchService bloodSearchService;

    /**
     * GET /api/search/blood?bloodGroup=O+&page=0&size=20
     *
     * Returns paginated availability across all active hospitals.
     * Results are sorted by proximity to the requesting user's hospital,
     * falling back to quantity descending.
     * When no stock is found, suggests compatible donor blood groups.
     */
    @GetMapping("/blood")
    public ResponseEntity<ApiResponse<BloodSearchResponse>> searchBlood(
            @RequestParam String bloodGroup,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        // Validate blood group early for a clear 400 message
        if (!BloodSearchPort.validBloodGroups().contains(bloodGroup)) {
            throw new IllegalArgumentException(
                    "Invalid blood group '" + bloodGroup + "'. Valid values: " + BloodSearchPort.validBloodGroups());
        }

        if (page < 0) throw new IllegalArgumentException("Page index must not be negative");
        if (size < 1 || size > 100) throw new IllegalArgumentException("Page size must be between 1 and 100");

        BloodSearchResponse result = bloodSearchService.search(bloodGroup, page, size, auth);

        String message = result.results().isEmpty()
                ? "No availability found for " + bloodGroup
                : "Found " + result.totalResults() + " result(s) for " + bloodGroup;

        return ResponseEntity.ok(ApiResponse.ok(message, result));
    }
}
