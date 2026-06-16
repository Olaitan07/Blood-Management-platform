package com.blood.donor.controller;

import com.blood.donor.dto.ApiResponse;
import com.blood.donor.dto.DonorRequest;
import com.blood.donor.dto.DonorResponse;
import com.blood.donor.service.DonorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
public class DonorController {

    private final DonorService donorService;

    @PostMapping
    public ResponseEntity<ApiResponse<DonorResponse>> registerDonor(
            @Valid @RequestBody DonorRequest request) {
        DonorResponse response = donorService.registerDonor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Donor registered successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DonorResponse>>> listDonors() {
        List<DonorResponse> donors = donorService.listDonors();
        return ResponseEntity.ok(ApiResponse.ok("Donors retrieved", donors));
    }
}
