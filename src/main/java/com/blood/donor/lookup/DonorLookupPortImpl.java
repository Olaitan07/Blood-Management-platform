package com.blood.donor.lookup;

import com.blood.donor.repository.DonorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class DonorLookupPortImpl implements DonorLookupPort {

    private final DonorRepository donorRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findDonorIdByEmail(String email) {
        return donorRepository.findByUserEmail(email).map(donor -> donor.getId());
    }
}
