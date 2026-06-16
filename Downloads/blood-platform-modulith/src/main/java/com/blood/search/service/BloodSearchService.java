package com.blood.search.service;

import com.blood.search.dto.BloodSearchResponse;
import org.springframework.security.core.Authentication;

public interface BloodSearchService {

    BloodSearchResponse search(String bloodGroup, int page, int size, Authentication auth);
}
