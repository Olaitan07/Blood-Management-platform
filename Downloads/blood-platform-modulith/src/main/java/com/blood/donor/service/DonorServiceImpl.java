package com.blood.donor.service;

import com.blood.donor.dto.DonorRequest;
import com.blood.donor.dto.DonorResponse;
import com.blood.donor.event.DonorRegisteredEvent;
import com.blood.donor.model.Donor;
import com.blood.donor.repository.DonorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class DonorServiceImpl implements DonorService {

    private final DonorRepository donorRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public DonorResponse registerDonor(DonorRequest request) {
        Donor donor = Donor.builder()
                .fullName(request.fullName())
                .bloodType(request.bloodType())
                .build();

        Donor saved = donorRepository.save(donor);

        applicationEventPublisher.publishEvent(new DonorRegisteredEvent(
                saved.getId(),
                saved.getFullName(),
                saved.getBloodType(),
                LocalDateTime.now().toString()
        ));

        log.info("Donor registered: {} with blood type {}", saved.getFullName(), saved.getBloodType());
        return DonorResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonorResponse> listDonors() {
        return donorRepository.findAll().stream()
                .map(DonorResponse::from)
                .toList();
    }
}
