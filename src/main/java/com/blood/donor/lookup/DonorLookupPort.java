package com.blood.donor.lookup;

import java.util.Optional;

/**
 * Narrow read-only port for other modules that need to resolve a donor's own
 * id from their authenticated identity (e.g. notification ownership checks),
 * without depending on the donor module's internal repository/service.
 */
public interface DonorLookupPort {

    Optional<Long> findDonorIdByEmail(String email);
}
