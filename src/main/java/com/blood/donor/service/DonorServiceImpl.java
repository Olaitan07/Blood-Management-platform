package com.blood.donor.service;

import com.blood.donor.dto.DonationRequest;
import com.blood.donor.dto.DonationResponse;
import com.blood.donor.dto.DonorRequest;
import com.blood.donor.dto.DonorResponse;
import com.blood.donor.event.DonationRecordedEvent;
import com.blood.donor.event.DonorRegisteredEvent;
import com.blood.donor.exception.DonorNotFoundException;
import com.blood.donor.exception.DuplicatePhoneException;
import com.blood.donor.model.Donation;
import com.blood.donor.model.Donor;
import com.blood.donor.model.EligibilityStatus;
import com.blood.donor.repository.DonationRepository;
import com.blood.donor.repository.DonorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class DonorServiceImpl implements DonorService {

    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EligibilityCalculator eligibilityCalculator;
    private final Clock clock;

    @Override
    @Transactional
    public DonorResponse registerDonor(DonorRequest request, String currentUserEmail) {
        if (donorRepository.existsByPhone(request.phone())) {
            throw new DuplicatePhoneException(request.phone());
        }

        Donor donor = Donor.builder()
                .fullName(request.fullName())
                .bloodGroup(request.bloodGroup())
                .phone(request.phone())
                .userEmail(currentUserEmail)
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .build();

        Donor saved = donorRepository.save(donor);

        // Spring Modulith persists this event in event_publication within the same transaction.
        // If the app crashes before delivery, the event is retried on restart (outbox pattern).
        eventPublisher.publishEvent(new DonorRegisteredEvent(
                saved.getId(),
                saved.getFullName(),
                saved.getBloodGroup().getValue(),
                Instant.now(clock)
        ));

        log.info("Donor registered: id={} bloodGroup={}", saved.getId(), saved.getBloodGroup().getValue());
        return DonorResponse.from(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public DonorResponse getMyProfile(String currentUserEmail) {
        Donor donor = findByEmailOrThrow(currentUserEmail);
        LocalDate eligibleFrom = eligibilityCalculator.eligibleFrom(donor.getLastDonationDate());
        return DonorResponse.from(donor, eligibleFrom);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonorResponse> listDonors() {
        return donorRepository.findAll().stream()
                .map(d -> DonorResponse.from(d, eligibilityCalculator.eligibleFrom(d.getLastDonationDate())))
                .toList();
    }

    @Override
    @Transactional
    public DonationResponse recordDonation(Long donorId, DonationRequest request, String currentUserEmail) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new DonorNotFoundException(donorId));

        // IDOR protection: donors can only record their own donations
        if (!donor.getUserEmail().equals(currentUserEmail)) {
            throw new AccessDeniedException("You can only record donations for your own profile");
        }

        LocalDate today = LocalDate.now(clock);

        if (request.donationDate().isAfter(today)) {
            throw new IllegalArgumentException("Donation date cannot be in the future");
        }

        // Reject out-of-order entries
        donationRepository.findTopByDonorOrderByDonationDateDesc(donor)
                .ifPresent(latest -> {
                    if (!request.donationDate().isAfter(latest.getDonationDate())) {
                        throw new IllegalArgumentException(
                                "Donation date must be after the previous donation date: " + latest.getDonationDate());
                    }
                });

        Donation donation = Donation.builder()
                .donor(donor)
                .donationDate(request.donationDate())
                .hospitalName(request.hospitalName())
                .units(request.units())
                .build();

        donationRepository.save(donation);

        // Update donor's last donation date and recalculate eligibility
        donor.setLastDonationDate(request.donationDate());
        donor.setEligibilityStatus(eligibilityCalculator.compute(request.donationDate()));
        donorRepository.save(donor);

        eventPublisher.publishEvent(new DonationRecordedEvent(
                donor.getId(),
                donor.getFullName(),
                donor.getBloodGroup().getValue(),
                request.donationDate(),
                Instant.now(clock)
        ));

        log.info("Donation recorded: donorId={} date={}", donor.getId(), request.donationDate());
        return DonationResponse.from(donation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DonationResponse> getDonationHistory(Long donorId, String currentUserEmail, Pageable pageable) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new DonorNotFoundException(donorId));

        // IDOR protection: donors can only view their own history
        if (!donor.getUserEmail().equals(currentUserEmail)) {
            throw new AccessDeniedException("You can only view your own donation history");
        }

        return donationRepository.findByDonorOrderByDonationDateDesc(donor, pageable)
                .map(DonationResponse::from);
    }

    private Donor findByEmailOrThrow(String email) {
        return donorRepository.findByUserEmail(email)
                .orElseThrow(() -> new DonorNotFoundException("No donor profile found for current user"));
    }
}
