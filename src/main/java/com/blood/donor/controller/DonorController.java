package com.blood.donor.controller;

import com.blood.donor.dto.ApiResponse;
import com.blood.donor.dto.DonationRequest;
import com.blood.donor.dto.DonationResponse;
import com.blood.donor.dto.DonorRequest;
import com.blood.donor.dto.DonorResponse;
import com.blood.donor.service.DonorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
public class DonorController {

    private final DonorService donorService;

    @PostMapping
    public ResponseEntity<ApiResponse<DonorResponse>> registerDonor(
            @Valid @RequestBody DonorRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        DonorResponse response = donorService.registerDonor(request, currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Donor registered successfully", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DonorResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails currentUser) {
        DonorResponse response = donorService.getMyProfile(currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Donor profile retrieved", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DonorResponse>>> listDonors() {
        List<DonorResponse> donors = donorService.listDonors();
        return ResponseEntity.ok(ApiResponse.ok("Donors retrieved", donors));
    }

    @PostMapping("/{id}/donations")
    public ResponseEntity<ApiResponse<DonationResponse>> recordDonation(
            @PathVariable Long id,
            @Valid @RequestBody DonationRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        DonationResponse response = donorService.recordDonation(id, request, currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Donation recorded successfully", response));
    }

    @GetMapping("/{id}/donations")
    public ResponseEntity<ApiResponse<Page<DonationResponse>>> getDonationHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser,
            @PageableDefault(size = 20, sort = "donationDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DonationResponse> history = donorService.getDonationHistory(id, currentUser.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Donation history retrieved", history));
    }
}
