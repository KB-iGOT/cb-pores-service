package com.igot.cb.cios.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.pores.util.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Request of the v2 onboard API. It carries the same pieces as one element of the v1 onboard request: the course in
 * {@code contentData}, the partner in {@code contentPartner}, the status and the optional extras. {@code contentData} is
 * one item exactly as the Deep Linking return API returns it: iGOT content already mapped by the partner's transform
 * spec, shaped as {@code {"content": {...}}}.
 */
@Getter
@Setter
@NoArgsConstructor
public class OnboardContentV2Request {
    private String partnerCode;
    private String status;
    private JsonNode contentData;
    /**
     * Optional: when sent it is stored as it is; otherwise it is built from the partner record.
     */
    private JsonNode contentPartner;
    @JsonProperty(Constants.COMPETENCIES_V5)
    private JsonNode competenciesV5;
    @JsonProperty(Constants.COMPETENCIES_V6)
    private JsonNode competenciesV6;
    private List<String> tags;
    private String difficultyLevel;
    private boolean accessSettingsEnabled;
    @JsonProperty(Constants.BADGE_DETAILS_V1)
    private JsonNode badgeDetailsV1;
    private String courseType;
    private Integer courseEnrolLimit;
    private Integer requiredKarmaPoints;

    /**
     * The partner the course belongs to: the top-level {@code partnerCode}, or, as in the v1 request that has none, the
     * {@code partnerCode} inside {@code contentPartner}. Null when the request carries neither.
     */
    public String getPartnerCode() {
        if (StringUtils.isNotBlank(partnerCode)) {
            return partnerCode;
        }
        return hasContentPartner() ? StringUtils.trimToNull(contentPartner.path(Constants.PARTNERCODE).asText(null)) : null;
    }

    /**
     * True when the request carries a {@code contentPartner}; JSON null counts as not sent.
     */
    public boolean hasContentPartner() {
        return contentPartner != null && !contentPartner.isNull();
    }
}
