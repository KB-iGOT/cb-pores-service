package com.igot.cb.contentpartner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.util.ApiResponse;

public interface ContentPartnerRegistrationService {

    ApiResponse upsert(JsonNode partnerDetails,String token);
    ApiResponse read(String id,String token);

    ApiResponse searchEntity(SearchCriteria searchCriteria,String token);
}