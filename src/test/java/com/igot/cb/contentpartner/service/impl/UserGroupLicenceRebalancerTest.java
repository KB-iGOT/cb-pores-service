package com.igot.cb.contentpartner.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.pores.Service.OutboundRequestHandlerServiceImpl;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserGroupLicenceRebalancerTest {

    private static final String URL = "http://ext-course/rebalance";

    @Mock
    private CbServerProperties cbServerProperties;
    @Mock
    private OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    private final ObjectMapper mapper = new ObjectMapper();
    private UserGroupLicenceRebalancer rebalancer;

    @BeforeEach
    void setUp() {
        rebalancer = new UserGroupLicenceRebalancer(cbServerProperties, outboundRequestHandlerService);
        lenient().when(cbServerProperties.getCbExtCourseServiceHost()).thenReturn("http://ext-course");
        lenient().when(cbServerProperties.getProviderAccessSettingsRebalancePath()).thenReturn("/rebalance");
    }

    private ObjectNode partnerData(Boolean providerPreference, Integer overAllLimit) {
        ObjectNode data = mapper.createObjectNode();
        if (providerPreference != null) {
            data.put(Constants.PROVIDER_PREFERENCE, providerPreference);
        }
        if (overAllLimit != null) {
            data.put(Constants.OVER_ALL_LIMIT, overAllLimit);
        }
        return data;
    }

    private void mockRebalanceResponse(Object response) {
        when(outboundRequestHandlerService.fetchResultUsingPost(eq(URL), any())).thenReturn(response);
    }

    private void verifyRebalanceRequest(long overAllLimit) {
        verify(outboundRequestHandlerService).fetchResultUsingPost(URL,
                Map.of(Constants.PARTNER_ID, "p1", Constants.OVER_ALL_LIMIT, overAllLimit));
    }

    @Test
    void preferenceSwitchedOn_rebalancesToTheStoredLimit() {
        mockRebalanceResponse(Map.of(Constants.RESPONSE_CODE, Constants.OK));

        assertTrue(rebalancer.rebalanceIfNeeded("p1", partnerData(null, 40000), partnerData(true, null)));

        verifyRebalanceRequest(40000L);
    }

    @Test
    void preferenceSwitchedOn_rebalancesToTheLimitBeingSaved() {
        mockRebalanceResponse(Map.of(Constants.RESPONSE_CODE, "ok"));

        assertTrue(rebalancer.rebalanceIfNeeded("p1", partnerData(false, 40000), partnerData(true, 500)));

        verifyRebalanceRequest(500L);
    }

    @Test
    void limitLoweredWhilePreferenceOn_rebalancesToTheNewLimit() {
        mockRebalanceResponse(Map.of(Constants.RESPONSE_CODE, Constants.OK));

        assertTrue(rebalancer.rebalanceIfNeeded("p1", partnerData(true, 100), partnerData(null, 50)));

        verifyRebalanceRequest(50L);
    }

    @Test
    void noRebalance_whenNothingCanOverPromise() {
        // Limit raised or unchanged while on.
        assertTrue(rebalancer.rebalanceIfNeeded("p1", partnerData(true, 100), partnerData(true, 150)));
        assertTrue(rebalancer.rebalanceIfNeeded("p1", partnerData(true, 100), partnerData(null, null)));
        // Limit lowered while off, preference switched off, or never set.
        assertTrue(rebalancer.rebalanceIfNeeded("p1", partnerData(false, 100), partnerData(null, 50)));
        assertTrue(rebalancer.rebalanceIfNeeded("p1", partnerData(true, 100), partnerData(false, 50)));
        assertTrue(rebalancer.rebalanceIfNeeded("p1", partnerData(null, 100), partnerData(null, 50)));

        verifyNoInteractions(outboundRequestHandlerService);
    }

    @Test
    void rebalanceWithoutResponse_fails() {
        mockRebalanceResponse(null);

        assertFalse(rebalancer.rebalanceIfNeeded("p1", partnerData(false, 100), partnerData(true, null)));
    }

    @Test
    void rebalanceNotOk_fails() {
        mockRebalanceResponse(Map.of(Constants.RESPONSE_CODE, HttpStatus.BAD_REQUEST.name()));

        assertFalse(rebalancer.rebalanceIfNeeded("p1", partnerData(true, 100), partnerData(true, 50)));
    }
}
