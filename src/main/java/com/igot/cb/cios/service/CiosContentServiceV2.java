package com.igot.cb.cios.service;

import com.igot.cb.cios.dto.OnboardContentV2Request;
import com.igot.cb.pores.util.ApiResponse;

public interface CiosContentServiceV2 {

    /**
     * Onboards one raw partner item: transforms it with the partner's saved spec, completes the mandatory fields,
     * then saves it as a draft or publishes it, exactly like the v1 onboard flow.
     */
    ApiResponse onboardContent(OnboardContentV2Request request);
}
