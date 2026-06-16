package com.blood.hospital.controller;

import com.blood.hospital.dto.ApiResponse;
import com.blood.hospital.dto.HospitalRequest;
import com.blood.hospital.dto.HospitalResponse;
import com.blood.hospital.service.HospitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HospitalResponse>> registerHospital(
            @Valid @RequestBody HospitalRequest request) {
        HospitalResponse response = hospitalService.registerHospital(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Hospital registered successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HospitalResponse>>> listHospitals(
            @RequestParam(defaultValue = "false") boolean all) {
        List<HospitalResponse> hospitals = all
                ? hospitalService.listAllHospitals()
                : hospitalService.listActiveHospitals();
        return ResponseEntity.ok(ApiResponse.ok("Hospitals retrieved", hospitals));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospital(@PathVariable Long id) {
        HospitalResponse response = hospitalService.getHospitalById(id);
        return ResponseEntity.ok(ApiResponse.ok("Hospital retrieved", response));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HospitalResponse>> deactivateHospital(@PathVariable Long id) {
        HospitalResponse response = hospitalService.deactivateHospital(id);
        return ResponseEntity.ok(ApiResponse.ok("Hospital deactivated successfully", response));
    }
}
