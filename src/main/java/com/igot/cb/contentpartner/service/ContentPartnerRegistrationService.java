package com.igot.cb.contentpartner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.pores.util.ApiResponse;

public interface ContentPartnerRegistrationService {

    ApiResponse createOrUpdate(JsonNode partnerDetails);
    ApiResponse read(String id);
}