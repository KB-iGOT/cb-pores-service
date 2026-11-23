package com.igot.cb.cios.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.igot.cb.cios.dto.OnboardContentV2Request;
import com.igot.cb.pores.exceptions.CustomException;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Everything that completes the content of a course before it is saved, for any partner and without partner specific
 * code. The service calls its three public methods in this order:
 *
 * <ol>
 *   <li>{@link #rejectMissingMandatoryFields}: the fields the publish form makes mandatory must be in the request,
 *       otherwise a 400. Nothing is added for them. The fields are listed in
 *       the mandatory fields file (property lti.content.onboard.mandatory.fields.path).</li>
 *   <li>{@link #addDefaultsForMissingFields}: the fields of the Transform Content form that the partner did not send
 *       get a default, so they never cause a 400. The defaults are listed in the default values file (property lti.content.onboard.default.values.path).
 *       Each entry has a {@code useDefault} flag, an optional {@code value} and optional {@code derive} sources such as
 *       {@code partner.link}, {@code content.name} or {@code generated.uuid}.</li>
 *   <li>{@link #assignPlatformFields}: the fields only the platform owns: content id, status, update date, partner
 *       summary, search tags and access settings.</li>
 * </ol>
 *
 * <p>Each file is read from the location in {@link CbServerProperties} (set in application.properties), so a list can be replaced without a new build. A value that is
 * already in the content is never replaced by a default.</p>
 */
@Slf4j
@Component
public class LtiContentFieldsHelper {

    private static final List<String> PARTNER_SUMMARY_FIELDS =
            List.of(Constants.PARTNER_LINK, Constants.IS_ACTIVE, Constants.WEBSITE_URL, Constants.DESCRIPTION,
                    Constants.PARTNER_DOCUMENT_URL, Constants.CONTENT_PARTNER_NAME, Constants.PROVIDER_TIPS);

    private static final ResourceLoader RESOURCE_LOADER =
            new DefaultResourceLoader(LtiContentFieldsHelper.class.getClassLoader());

    private final ObjectMapper objectMapper;
    private final List<MandatoryField> mandatoryFields;
    private final Map<String, FieldRule> defaultRules;
    private final JsonSchema liveContentSchema;
    private final AtomicInteger contentIdSequence = new AtomicInteger(1);

    /**
     * @param objectMapper       mapper used to read the two configuration files and the live content schema
     * @param cbServerProperties supplies the optional paths of files that replace the packaged configuration files
     */
    public LtiContentFieldsHelper(ObjectMapper objectMapper, CbServerProperties cbServerProperties) {
        this.objectMapper = objectMapper;
        log.info("LtiContentFieldsHelper: reading mandatory fields from {} and default values from {}",
                cbServerProperties.getLtiContentOnboardMandatoryFieldsPath(),
                cbServerProperties.getLtiContentOnboardDefaultValuesPath());
        this.mandatoryFields = loadMandatoryFields(cbServerProperties.getLtiContentOnboardMandatoryFieldsPath());
        this.defaultRules = loadDefaultRules(cbServerProperties.getLtiContentOnboardDefaultValuesPath());
        this.liveContentSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(readPackagedJson(Constants.CIOS_CONTENT_VALIDATION_FILE_JSON));
    }

    // ---------------------------------------------------------------- mandatory fields of the request

    /**
     * @param content the course content with everything the request sent already applied
     * @throws CustomException with status 400 naming every mandatory field that is missing
     */
    public void rejectMissingMandatoryFields(ObjectNode content) {
        List<String> missing = mandatoryFields.stream()
                .filter(field -> field.appliesTo(content))
                .map(MandatoryField::name)
                .filter(name -> isMissingMandatoryValue(content.path(name)))
                .toList();
        if (!missing.isEmpty()) {
            log.warn("LtiContentFieldsHelper::rejectMissingMandatoryFields: externalId {} lacks {}",
                    content.path(Constants.EXTERNAL_ID).asText(), missing);
            throw new CustomException(Constants.ERROR, Constants.MANDATORY_FIELDS_MISSING + String.join(", ", missing),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * A mandatory value is missing when it is absent, null, blank, an empty list or an empty object.
     */
    private static boolean isMissingMandatoryValue(JsonNode value) {
        return value.isMissingNode() || value.isNull() || (value.isTextual() && value.asText().isBlank())
                || ((value.isArray() || value.isObject()) && value.isEmpty());
    }

    // ---------------------------------------------------------------- defaults for the missing fields

    /**
     * Adds the default for every listed field the content lacks, then checks the mandatory fields of the live schema.
     *
     * @param content     the transformed content object, completed in place
     * @param partnerData the partner's {@code data} node, used by {@code derive} sources; may be null
     * @return the names of the fields that were added
     * @throws CustomException with status 400 when a mandatory field is still missing after the defaults were added
     */
    public List<String> addDefaultsForMissingFields(ObjectNode content, JsonNode partnerData) {
        JsonNode partner = partnerData == null ? MissingNode.getInstance() : partnerData;
        removeEmptyValues(content);
        List<String> added = addConfiguredDefaults(content, partner);
        rejectMandatoryFieldsStillMissing(content);
        return added;
    }

    /**
     * A null or blank value counts as missing, so it can be filled like an absent one.
     */
    private void removeEmptyValues(ObjectNode content) {
        content.properties().removeIf(entry -> isBlankValue(entry.getValue()));
    }

    private static boolean isBlankValue(JsonNode value) {
        return value.isMissingNode() || value.isNull() || (value.isTextual() && value.asText().isBlank());
    }

    /**
     * Derived values first, repeated so a field may derive from another derived one; fixed values last.
     */
    private List<String> addConfiguredDefaults(ObjectNode content, JsonNode partner) {
        List<FieldRule> pending = new ArrayList<>(defaultRules.values().stream()
                .filter(rule -> rule.useDefault() && !content.has(rule.name()))
                .toList());
        List<String> added = new ArrayList<>();
        boolean progressed;
        do {
            progressed = pending.removeIf(rule -> addValueDerivedFromOtherFields(content, partner, rule, added));
        } while (progressed);
        pending.stream().filter(FieldRule::hasValue).forEach(rule -> {
            content.set(rule.name(), rule.value().deepCopy());
            added.add(rule.name());
        });
        return added;
    }

    private boolean addValueDerivedFromOtherFields(ObjectNode content, JsonNode partner, FieldRule rule,
                                                   List<String> added) {
        Optional<JsonNode> derived = rule.derive().stream()
                .map(source -> valueFromSource(source, content, partner))
                .filter(value -> !isBlankValue(value))
                .findFirst();
        derived.ifPresent(value -> {
            content.set(rule.name(), value.deepCopy());
            added.add(rule.name());
        });
        return derived.isPresent();
    }

    /**
     * Sources look like {@code partner.contentPartnerName}, {@code content.otherField} or {@code generated.uuid}.
     */
    private static JsonNode valueFromSource(String source, JsonNode content, JsonNode partner) {
        if (source.startsWith(Constants.DERIVE_PARTNER_SOURCE_PREFIX)) {
            return partner.path(source.substring(Constants.DERIVE_PARTNER_SOURCE_PREFIX.length()));
        }
        if (source.startsWith(Constants.DERIVE_CONTENT_SOURCE_PREFIX)) {
            return content.path(source.substring(Constants.DERIVE_CONTENT_SOURCE_PREFIX.length()));
        }
        if (source.equals(Constants.DERIVE_GENERATED_SOURCE_PREFIX + Constants.GENERATED_UUID_SOURCE)) {
            return TextNode.valueOf(UUID.randomUUID().toString());
        }
        return MissingNode.getInstance();
    }

    /**
     * Asks the live content schema which mandatory fields are absent; it also knows the paid-course rule.
     */
    private void rejectMandatoryFieldsStillMissing(ObjectNode content) {
        ObjectNode root = objectMapper.createObjectNode();
        root.set(Constants.CONTENT, content);
        List<String> missing = liveContentSchema.validate(root).stream()
                .filter(message -> Constants.SCHEMA_REQUIRED_KEYWORD.equals(message.getType()))
                .map(message -> message.getArguments()[0])
                .distinct()
                .toList();
        if (!missing.isEmpty()) {
            log.warn("LtiContentFieldsHelper::rejectMandatoryFieldsStillMissing: externalId {} still lacks {} after the defaults",
                    content.path(Constants.EXTERNAL_ID).asText(), missing);
            throw new CustomException(Constants.ERROR, Constants.KEY_FIELDS_MISSING + String.join(", ", missing),
                    HttpStatus.BAD_REQUEST);
        }
    }

    // ---------------------------------------------------------------- fields owned by the platform

    /**
     * Sets the fields only the platform owns. The partner summary is stored as the request sent it, and built from
     * the partner record only when the request sent none.
     */
    public void assignPlatformFields(ObjectNode content, OnboardContentV2Request request, JsonNode partner,
                                     String status, Timestamp now) {
        content.put(Constants.STATUS, status);
        content.put(Constants.UPDATED_DATE, now.toString());
        content.put(Constants.CONTENT_ID, contentIdOrNew(content));
        content.set(Constants.CONTENT_PARTNER, contentPartnerForCourse(request, partner));
        content.set(Constants.SEARCHTAGS, buildSearchTags(content, request.getTags()));
        content.set(Constants.ACCESS_SETTINGS_ENABLED, BooleanNode.valueOf(request.isAccessSettingsEnabled()));
    }

    private String contentIdOrNew(ObjectNode content) {
        String existing = content.path(Constants.CONTENT_ID).asText(null);
        return StringUtils.isBlank(existing) ? generateContentId() : existing;
    }

    private String generateContentId() {
        return Constants.ID_PREFIX + Constants.CONTENT_ID_ENVIRONMENT
                + (System.currentTimeMillis() << Constants.CONTENT_ID_TIME_SHIFT)
                + Constants.CONTENT_ID_SHARD + contentIdSequence.getAndIncrement();
    }

    private JsonNode contentPartnerForCourse(OnboardContentV2Request request, JsonNode partner) {
        return request.hasContentPartner()
                ? request.getContentPartner().deepCopy()
                : contentPartnerFromPartnerRecord(partner, request.getPartnerCode());
    }

    /**
     * The partner's id and code plus whichever display fields it has; the provider tips are always present.
     */
    private ObjectNode contentPartnerFromPartnerRecord(JsonNode partner, String partnerCode) {
        JsonNode partnerData = partner.path(Constants.DATA);
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put(Constants.ID, partner.path(Constants.ID).asText());
        summary.put(Constants.PARTNERCODE, partnerCode);
        PARTNER_SUMMARY_FIELDS.forEach(field -> {
            JsonNode value = partnerData.get(field);
            if (value != null && !value.isNull()) {
                summary.set(field, value);
            }
        });
        if (!summary.has(Constants.PROVIDER_TIPS)) {
            summary.putArray(Constants.PROVIDER_TIPS);
        }
        return summary;
    }

    /**
     * The course name first, then the requested tags, all lower case, without blanks or repeats.
     */
    private ArrayNode buildSearchTags(ObjectNode content, List<String> tags) {
        Set<String> searchTags = new LinkedHashSet<>();
        searchTags.add(StringUtils.lowerCase(content.path(Constants.NAME).asText()));
        if (tags != null) {
            tags.stream().filter(StringUtils::isNotBlank).map(StringUtils::lowerCase).forEach(searchTags::add);
        }
        return objectMapper.valueToTree(searchTags);
    }

    // ---------------------------------------------------------------- reading the configuration

    private List<MandatoryField> loadMandatoryFields(String location) {
        try (InputStream stream = openConfigFile(location)) {
            return objectMapper.readValue(stream, MandatoryFields.class).fields();
        } catch (IOException e) {
            log.error("LtiContentFieldsHelper::loadMandatoryFields: cannot read {}", location, e);
            throw new UncheckedIOException("Unable to load the onboard mandatory fields", e);
        }
    }

    private Map<String, FieldRule> loadDefaultRules(String location) {
        try (InputStream stream = openConfigFile(location)) {
            Map<String, FieldRule> byName = new LinkedHashMap<>();
            objectMapper.readValue(stream, OnboardDefaults.class).fields()
                    .forEach(rule -> byName.put(rule.name(), rule));
            return byName;
        } catch (IOException e) {
            log.error("LtiContentFieldsHelper::loadDefaultRules: cannot read {}", location, e);
            throw new UncheckedIOException("Unable to load the onboard defaults", e);
        }
    }

    private JsonNode readPackagedJson(String path) {
        try (InputStream stream = openPackagedFile(path)) {
            return objectMapper.readValue(stream, JsonNode.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load " + path, e);
        }
    }

    /**
     * Opens the configuration file at a Spring location, such as classpath:/... or file:/...
     */
    private static InputStream openConfigFile(String location) throws IOException {
        return RESOURCE_LOADER.getResource(location).getInputStream();
    }

    private static InputStream openPackagedFile(String packagedPath) {
        return Objects.requireNonNull(LtiContentFieldsHelper.class.getResourceAsStream(packagedPath),
                "Missing " + packagedPath);
    }

    // ---------------------------------------------------------------- shapes of the configuration files

    /**
     * One mandatory field of the request.
     *
     * @param name     the content field
     * @param onlyWhen optional conditions, each a field and the value it must have for this entry to apply
     */
    record MandatoryField(String name, Map<String, String> onlyWhen) {

        MandatoryField {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Every mandatory field needs a name");
            }
            onlyWhen = onlyWhen == null ? Map.of() : Map.copyOf(onlyWhen);
        }

        boolean appliesTo(JsonNode content) {
            return onlyWhen.entrySet().stream()
                    .allMatch(condition -> condition.getValue().equalsIgnoreCase(content.path(condition.getKey()).asText("")));
        }
    }

    /**
     * Shape of the mandatory fields file (property lti.content.onboard.mandatory.fields.path).
     */
    record MandatoryFields(List<MandatoryField> fields) {

        MandatoryFields {
            fields = fields == null ? List.of() : List.copyOf(fields);
        }
    }

    /**
     * One field of the defaults file.
     *
     * @param name       the content field
     * @param useDefault true to add the field when missing, false to never add it
     * @param value      the value to add when nothing can be derived; absent when the field has no fixed default
     * @param derive     sources tried in order before {@code value}, such as {@code partner.link}
     */
    record FieldRule(String name, Boolean useDefault, JsonNode value, List<String> derive) {

        FieldRule {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Every onboard default needs a field name");
            }
            if (useDefault == null) {
                throw new IllegalStateException("useDefault must be true or false for field \"" + name + "\"");
            }
            derive = derive == null ? List.of() : List.copyOf(derive);
        }

        boolean hasValue() {
            return value != null && !value.isNull();
        }
    }

    /**
     * Shape of the default values file (property lti.content.onboard.default.values.path).
     */
    record OnboardDefaults(List<FieldRule> fields) {

        OnboardDefaults {
            fields = fields == null ? List.of() : List.copyOf(fields);
            if (fields.stream().map(FieldRule::name).distinct().count() != fields.size()) {
                throw new IllegalStateException("A field is listed twice in the onboard defaults");
            }
        }
    }
}
