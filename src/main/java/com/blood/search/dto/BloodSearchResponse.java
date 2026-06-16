package com.blood.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BloodSearchResponse(
        String bloodGroup,
        int page,
        int size,
        long totalResults,
        int totalPages,
        List<BloodSearchResult> results,
        List<String> suggestions   // non-null only when results is empty
) {}
