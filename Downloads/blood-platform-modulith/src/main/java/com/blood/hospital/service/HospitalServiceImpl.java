package com.blood.hospital.service;

import com.blood.hospital.dto.HospitalRequest;
import com.blood.hospital.dto.HospitalResponse;
import com.blood.hospital.event.HospitalDeactivatedEvent;
import com.blood.hospital.event.HospitalRegisteredEvent;
import com.blood.hospital.exception.DuplicateHospitalException;
import com.blood.hospital.exception.HospitalNotFoundException;
import com.blood.hospital.model.Hospital;
import com.blood.hospital.model.HospitalStatus;
import com.blood.hospital.repository.HospitalRepository;
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
class HospitalServiceImpl implements HospitalService {

    private final HospitalRepository hospitalRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public HospitalResponse registerHospital(HospitalRequest request) {
        hospitalRepository.findByNameIgnoreCaseAndCityIgnoreCase(request.name(), request.city())
                .ifPresent(existing -> {
                    throw new DuplicateHospitalException(
                            "Hospital already exists with name '" + request.name() +
                            "' in city '" + request.city() + "'");
                });

        Hospital hospital = Hospital.builder()
                .name(request.name())
                .address(request.address())
                .state(request.state())
                .city(request.city())
                .contact(request.contact())
                .status(HospitalStatus.ACTIVE)
                .build();

        Hospital saved = hospitalRepository.save(hospital);

        eventPublisher.publishEvent(new HospitalRegisteredEvent(
                saved.getId(),
                saved.getName(),
                saved.getCity(),
                saved.getState(),
                saved.getContact(),
                LocalDateTime.now()
        ));

        log.info("Hospital registered: {} in {}", saved.getName(), saved.getCity());
        return HospitalResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalResponse> listActiveHospitals() {
        return hospitalRepository.findByStatus(HospitalStatus.ACTIVE).stream()
                .map(HospitalResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalResponse> listAllHospitals() {
        return hospitalRepository.findAll().stream()
                .map(HospitalResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalResponse getHospitalById(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException("Hospital not found with id: " + id));
        return HospitalResponse.from(hospital);
    }

    @Override
    @Transactional
    public HospitalResponse deactivateHospital(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new HospitalNotFoundException("Hospital not found with id: " + id));

        hospital.setStatus(HospitalStatus.INACTIVE);
        Hospital saved = hospitalRepository.save(hospital);

        eventPublisher.publishEvent(new HospitalDeactivatedEvent(
                saved.getId(),
                saved.getName(),
                saved.getCity(),
                LocalDateTime.now()
        ));

        log.info("Hospital deactivated: {} in {}", saved.getName(), saved.getCity());
        return HospitalResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hospitalExists(Long id) {
        return hospitalRepository.findById(id)
                .map(h -> h.getStatus() == HospitalStatus.ACTIVE)
                .orElse(false);
    }
}
