package com.igot.cb.contentpartner.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.pores.Service.OutboundRequestHandlerServiceImpl;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * With providerPreference true, the Over All Limit is distributed to the provider's user groups
 * (cb-ext-course-service). Used by the provider updates (v1 and v2) before the provider is saved.
 */
@Component
@Slf4j
public class UserGroupLicenceRebalancer {

    private final CbServerProperties cbServerProperties;
    private final OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    public UserGroupLicenceRebalancer(CbServerProperties cbServerProperties,
                                      OutboundRequestHandlerServiceImpl outboundRequestHandlerService) {
        this.cbServerProperties = cbServerProperties;
        this.outboundRequestHandlerService = outboundRequestHandlerService;
    }

    /**
     * When the preference is switched on, or the Over All Limit is lowered while it is on, rebalances
     * the group allocations to the Over All Limit being saved. A missing providerPreference is read
     * as false, and a missing value in the request keeps the stored one.
     *
     * @return false when the rebalance fails, so the update is refused
     */
    public boolean rebalanceIfNeeded(String partnerId, JsonNode existingData, JsonNode incomingData) {
        boolean oldPreference = existingData.path(Constants.PROVIDER_PREFERENCE).asBoolean(false);
        boolean newPreference = incomingData.path(Constants.PROVIDER_PREFERENCE).asBoolean(oldPreference);
        long oldLimit = existingData.path(Constants.OVER_ALL_LIMIT).asLong(0);
        long newLimit = incomingData.path(Constants.OVER_ALL_LIMIT).asLong(oldLimit);
        if (!newPreference || (oldPreference && newLimit >= oldLimit)) {
            return true;
        }
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.PARTNER_ID, partnerId);
        request.put(Constants.OVER_ALL_LIMIT, newLimit);
        String url = cbServerProperties.getCbExtCourseServiceHost() + cbServerProperties.getProviderAccessSettingsRebalancePath();
        Object rebalanceResponse = outboundRequestHandlerService.fetchResultUsingPost(url, request);
        if (rebalanceResponse instanceof Map<?, ?> responseMap
                && Constants.OK.equalsIgnoreCase(String.valueOf(responseMap.get(Constants.RESPONSE_CODE)))) {
            return true;
        }
        log.error("Failed to rebalance user group licences for partnerId: {}, response: {}", partnerId, rebalanceResponse);
        return false;
    }
}
