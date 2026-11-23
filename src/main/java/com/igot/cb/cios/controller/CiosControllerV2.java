package com.igot.cb.cios.controller;

import com.igot.cb.cios.dto.OnboardContentV2Request;
import com.igot.cb.cios.service.CiosContentServiceV2;
import com.igot.cb.pores.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cios/v2")
@RequiredArgsConstructor
public class CiosControllerV2 {

    private final CiosContentServiceV2 ciosContentServiceV2;

    @PostMapping("/LTI/onboardContent")
    public ResponseEntity<ApiResponse> onboardContent(@RequestBody OnboardContentV2Request request) {
        ApiResponse response = ciosContentServiceV2.onboardContent(request);
        return new ResponseEntity<>(response, response.getResponseCode());
    }
}
