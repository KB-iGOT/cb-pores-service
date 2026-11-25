package com.igot.cb.contentpartner;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.pores.exceptions.CustomException;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.PayloadValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the real contentProviderValidationV2Update.json through the real PayloadValidation
 * (the service tests mock PayloadValidation, so they never exercise the schema).
 */
class ContentPartnerV2SchemaTest {

    private static final String V2_SCHEMA = "/payloadValidation/contentProviderValidationV2Update.json";
    private static final List<String> ACCEPTED_URLS = List.of(
            "https://provider.example.com/launch",
            "http://provider.example.com/launch",
            "HTTPS://Provider.Example.com/x",
            "https://a.com:8443/x?a=1&b=2#frag",
            "http://10.0.0.5:8080/lti/login",
            "https://a.com");
    private static final List<String> REJECTED_URLS = List.of(
            "ftp://a.com/x", "javascript:alert(1)", "mailto:a@b.com", "file:///etc/passwd", "data:text/html,hi",
            "//a.com/x", "/just/a/path", "example.com/launch", "not a url", "",
            "https://a.com extra text", " https://a.com/x", "https://a.com/x ", "https://a.com/x\n", "https://a.com/x\nfoo",
            "https://", "http://", "https:// a.com", "https:///path", "https://a.com/<script>");
    private final ObjectMapper mapper = new ObjectMapper();
    private final PayloadValidation validation = new PayloadValidation();

    private ObjectNode basePartner() {
        ObjectNode node = mapper.createObjectNode();
        node.put("contentPartnerName", "Moodle Provider");
        node.put("description", "Moodle courses");
        node.put("websiteUrl", "https://moodle.example.com");
        node.put("link", "https://moodle.example.com/courses");
        return node;
    }

    /**
     * Request contract: only the endpoint URLs are supplied; the four other fields are server-managed.
     */
    private ObjectNode validLti() {
        ObjectNode lti = mapper.createObjectNode();
        lti.put("toolPublicJwksUrl", "https://provider.example.com/.well-known/jwks.json");
        lti.put("oidcLoginInitiationUrl", "https://provider.example.com/login");
        lti.put("redirectLaunchUrl", "https://provider.example.com/launch");
        lti.put("deepLinkingUrl", "https://provider.example.com/deep-link");
        return lti;
    }

    private ObjectNode ltiPartner(ObjectNode lti) {
        ObjectNode node = basePartner();
        node.put("protocol", "LTI");
        node.set("ltiConfiguration", lti);
        return node;
    }

    private void assertRejected(ObjectNode payload) {
        CustomException ex = assertThrows(CustomException.class, () -> validation.validatePayload(V2_SCHEMA, payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
    }

    @Test
    void payloadWithoutProtocolIsAcceptedBecauseProtocolIsOptional() {
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, basePartner()));
    }

    @Test
    void explicitSamlWithoutLtiConfigurationIsValid() {
        ObjectNode node = basePartner();
        node.put("protocol", "SAML");
        assertFalse(node.has("ltiConfiguration"));
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, node));
    }

    @Test
    void samlIgnoresLtiConfigurationContents() {
        ObjectNode empty = basePartner();
        empty.put("protocol", "SAML");
        empty.set("ltiConfiguration", mapper.createObjectNode());
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, empty));

        ObjectNode partial = basePartner();
        partial.put("protocol", "SAML");
        ObjectNode lti = mapper.createObjectNode();
        lti.put("redirectLaunchUrl", "https://provider.example.com/launch");
        partial.set("ltiConfiguration", lti);
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, partial));
    }

    @Test
    void ltiRequiresLtiConfiguration() {
        ObjectNode node = basePartner();
        node.put("protocol", "LTI");
        assertRejected(node);
    }

    @Test
    void validLtiIsAccepted() {
        ObjectNode payload = ltiPartner(validLti());
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, payload));
    }

    @Test
    void deepLinkingUrlIsOptional() {
        ObjectNode lti = validLti();
        lti.remove("deepLinkingUrl");
        ObjectNode payload = ltiPartner(lti);
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, payload));
    }

    @Test
    void ssoTestStatusAndMessageAreAcceptedAndOptional() {
        ObjectNode lti = validLti();
        lti.put("ssoTestStatus", "SUCCESS");
        lti.put("message", "All checks passed");
        ObjectNode payload = ltiPartner(lti);
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, payload));

        ObjectNode onlyStatus = validLti();
        onlyStatus.put("ssoTestStatus", "SUCCESS");
        ObjectNode onlyStatusPayload = ltiPartner(onlyStatus);
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, onlyStatusPayload));

        ObjectNode withoutBoth = ltiPartner(validLti());
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, withoutBoth));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ssoTestStatus", "message"})
    void ssoTestStatusAndMessageMustBeStrings(String field) {
        ObjectNode lti = validLti();
        lti.put(field, 5);
        assertRejected(ltiPartner(lti));
    }

    @Test
    void existingRequiredPartnerFieldsStillApply() {
        ObjectNode node = ltiPartner(validLti());
        node.remove("link");
        assertRejected(node);
        node = ltiPartner(validLti());
        node.remove("description");
        assertRejected(node);
    }

    @Test
    void serverManagedFieldsAreNotRequiredInTheRequest() {
        ObjectNode lti = validLti();
        assertFalse(lti.has("ltiVersion") || lti.has("toolIssuer") || lti.has("clientId") || lti.has("deploymentId"));
        ObjectNode payload = ltiPartner(lti);
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, payload));
    }

    /**
     * The frontend echoes the stored values back on update; the schema must let them through so the service can
     * compare them (it rejects any change with its own 400).
     */
    @ParameterizedTest
    @ValueSource(strings = {"ltiVersion", "toolIssuer", "clientId", "deploymentId"})
    void serverManagedFieldsAreAcceptedWhenSentAsStrings(String field) {
        ObjectNode lti = validLti();
        lti.put(field, "value");
        ObjectNode payload = ltiPartner(lti);
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, payload));
    }

    @Test
    void allFourServerManagedFieldsTogetherAreAccepted() {
        ObjectNode lti = validLti();
        lti.put("clientId", "portal");
        lti.put("ltiVersion", "1.3");
        lti.put("toolIssuer", "https://portal.dev.karmayogibharat.net/auth/realms/sunbird");
        lti.put("deploymentId", "IGOT-TEST-UHUWE");
        lti.put("ssoTestStatus", "Failed");
        lti.put("message", "The LTI tool could not recognize the platform registration.");
        ObjectNode payload = ltiPartner(lti);
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, payload));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ltiVersion", "toolIssuer", "clientId", "deploymentId"})
    void serverManagedFieldsMustBeStrings(String field) {
        ObjectNode lti = validLti();
        lti.put(field, true);
        assertRejected(ltiPartner(lti));
        lti.putObject(field).put("value", "x");
        assertRejected(ltiPartner(lti));
    }

    @ParameterizedTest
    @ValueSource(strings = {"toolPublicJwksUrl", "oidcLoginInitiationUrl", "redirectLaunchUrl"})
    void missingRequiredLtiFieldIsRejected(String field) {
        ObjectNode lti = validLti();
        lti.remove(field);
        assertRejected(ltiPartner(lti));
    }

    @ParameterizedTest
    @CsvSource({"ssoTestResult, SUCCESS", "unknown, x", "deepLinkingUrl, '   '"})
    void unexpectedFieldsAndBlankDeepLinkingUrlAreRejected(String field, String value) {
        ObjectNode lti = validLti();
        lti.put(field, value);
        assertRejected(ltiPartner(lti));
    }

    @Test
    void emptyLtiConfigurationIsRejected() {
        assertRejected(ltiPartner(mapper.createObjectNode()));
    }

    @Test
    void emptyDeepLinkingUrlIsAccepted() {
        ObjectNode lti = validLti();
        lti.put("deepLinkingUrl", "");
        ObjectNode payload = ltiPartner(lti);
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, payload));
    }

    @Test
    void nullDeepLinkingUrlIsAccepted() {
        ObjectNode lti = validLti();
        lti.putNull("deepLinkingUrl");
        ObjectNode payload = ltiPartner(lti);
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, payload));
    }

    @ParameterizedTest
    @ValueSource(strings = {"toolPublicJwksUrl", "oidcLoginInitiationUrl", "redirectLaunchUrl", "deepLinkingUrl"})
    void invalidLtiUrlIsRejected(String field) {
        ObjectNode lti = validLti();
        lti.put(field, "not a url");
        assertRejected(ltiPartner(lti));
    }

    @ParameterizedTest
    @ValueSource(strings = {"toolPublicJwksUrl", "oidcLoginInitiationUrl", "redirectLaunchUrl", "deepLinkingUrl"})
    void httpAndHttpsUrlsAreAcceptedInEveryLtiUrlField(String field) {
        for (String url : ACCEPTED_URLS) {
            if (isPlainHttp(url) && "deepLinkingUrl".equals(field)) {
                continue; // the deep linking URL accepts https only
            }
            ObjectNode lti = validLti();
            lti.put(field, url);
            ObjectNode acceptedPayload = ltiPartner(lti);
            assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, acceptedPayload), field + " should accept " + url);
        }
    }

    private static boolean isPlainHttp(String url) {
        return url.regionMatches(true, 0, "http://", 0, "http://".length());
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://provider.example.com/deep-link", "HTTP://Provider.Example.com/x",
            "http://10.0.0.5:8080/lti/deep-link"})
    void deepLinkingUrlOverPlainHttpIsRejected(String url) {
        ObjectNode lti = validLti();
        lti.put("deepLinkingUrl", url);
        assertRejected(ltiPartner(lti));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://provider.example.com/deep-link", "HTTPS://Provider.Example.com/x",
            "https://a.com:8443/x?a=1&b=2#frag"})
    void deepLinkingUrlOverHttpsIsAccepted(String url) {
        ObjectNode lti = validLti();
        lti.put("deepLinkingUrl", url);
        ObjectNode acceptedPayload = ltiPartner(lti);
        assertDoesNotThrow(() -> validation.validatePayload(V2_SCHEMA, acceptedPayload));
    }

    @ParameterizedTest
    @ValueSource(strings = {"toolPublicJwksUrl", "oidcLoginInitiationUrl", "redirectLaunchUrl", "deepLinkingUrl"})
    void anythingThatIsNotJustAnHttpOrHttpsUrlIsRejectedInEveryLtiUrlField(String field) {
        for (String url : REJECTED_URLS) {
            if (url.isEmpty() && "deepLinkingUrl".equals(field)) {
                continue; // an empty deepLinkingUrl means "not configured" and is accepted
            }
            ObjectNode lti = validLti();
            lti.put(field, url);
            ObjectNode payload = ltiPartner(lti);
            assertThrows(CustomException.class, () -> validation.validatePayload(V2_SCHEMA, payload),
                    field + " should reject [" + url + "]");
        }
    }

    @Test
    void wrongTypeAndNullLtiValuesAreRejected() {
        ObjectNode numeric = validLti();
        numeric.put("redirectLaunchUrl", 42);
        assertRejected(ltiPartner(numeric));
        ObjectNode nullValue = validLti();
        nullValue.putNull("toolPublicJwksUrl");
        assertRejected(ltiPartner(nullValue));
    }

    @Test
    void unsupportedOrNullProtocolIsRejected() {
        ObjectNode node = basePartner();
        node.put("protocol", "OAUTH");
        assertRejected(node);
        node.put("protocol", "lti");
        assertRejected(node);
        node.putNull("protocol");
        assertRejected(node);
    }

    @Test
    void ltiConfigurationMustBeAnObject() {
        ObjectNode node = basePartner();
        node.put("protocol", "LTI");
        node.put("ltiConfiguration", "text");
        assertRejected(node);
    }

    @Test
    void v1SchemaStillRejectsProtocolFields() {
        ObjectNode payload = ltiPartner(validLti());
        CustomException ex = assertThrows(CustomException.class,
                () -> validation.validatePayload(Constants.PAYLOAD_VALIDATION_FILE_CONTENT_PROVIDER, payload));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
    }
}
