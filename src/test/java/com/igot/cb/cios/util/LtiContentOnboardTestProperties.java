package com.igot.cb.cios.util;

import com.igot.cb.pores.util.CbServerProperties;

/** Builds the properties the helper reads its two files from, without starting Spring. */
public final class LtiContentOnboardTestProperties {

    private static final String CLASSPATH = "classpath:";

    private LtiContentOnboardTestProperties() {
    }

    /** Both locations point at the files packaged with the service, as the defaults in application.properties do. */
    public static CbServerProperties packaged() {
        CbServerProperties properties = new CbServerProperties();
        properties.setLtiContentOnboardDefaultValuesPath(CLASSPATH + "/payloadValidation/ltiContentOnboardDefaultValues.json");
        properties.setLtiContentOnboardMandatoryFieldsPath(CLASSPATH + "/payloadValidation/ltiContentOnboardMandatoryFields.json");
        return properties;
    }

    public static CbServerProperties withDefaultValuesLocation(String location) {
        CbServerProperties properties = packaged();
        properties.setLtiContentOnboardDefaultValuesPath(location);
        return properties;
    }

    public static CbServerProperties withMandatoryFieldsLocation(String location) {
        CbServerProperties properties = packaged();
        properties.setLtiContentOnboardMandatoryFieldsPath(location);
        return properties;
    }
}
