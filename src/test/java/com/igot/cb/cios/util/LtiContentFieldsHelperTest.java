package com.igot.cb.cios.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.cios.dto.OnboardContentV2Request;
import com.igot.cb.pores.exceptions.CustomException;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.PayloadValidation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests of the three public methods of {@link LtiContentFieldsHelper}, one group per method.
 */
class LtiContentFieldsHelperTest {

    @Nested
    class RejectMissingMandatoryFields {


        private static final String COMPETENCIES = "\"competencies_v6\":[{\"competencyAreaName\":\"Behavioural\"}]";

        private final ObjectMapper mapper = new ObjectMapper();
        private final LtiContentFieldsHelper validator = new LtiContentFieldsHelper(mapper, LtiContentOnboardTestProperties.packaged());

        private static CbServerProperties mandatoryPath(String location) {
            return LtiContentOnboardTestProperties.withMandatoryFieldsLocation(location);
        }

        private ObjectNode content(String json) throws Exception {
            return (ObjectNode) mapper.readTree(json);
        }

        private CustomException rejected(ObjectNode content) {
            return assertThrows(CustomException.class, () -> validator.rejectMissingMandatoryFields(content));
        }

        @Test
        void paidCourseWithTheFormFields_isAccepted() {
            assertDoesNotThrow(() -> validator.rejectMissingMandatoryFields(
                    content("{\"courseType\":\"paid\",\"requiredKarmaPoints\":50," + COMPETENCIES + "}")));
        }

        @Test
        void karmaPointsOfZero_countAsPresent() {
            assertDoesNotThrow(() -> validator.rejectMissingMandatoryFields(
                    content("{\"courseType\":\"paid\",\"requiredKarmaPoints\":0," + COMPETENCIES + "}")));
        }

        @Test
        void freeCourseWithoutKarmaPoints_isAccepted() {
            assertDoesNotThrow(() -> validator.rejectMissingMandatoryFields(
                    content("{\"courseType\":\"free\"," + COMPETENCIES + "}")));
        }

        @Test
        void paidCourseWithoutKarmaPoints_isABadRequest() throws Exception {
            CustomException e = rejected(content("{\"courseType\":\"PAID\"," + COMPETENCIES + "}"));

            assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatusCode());
            assertEquals(Constants.MANDATORY_FIELDS_MISSING + "requiredKarmaPoints", e.getMessage());
        }

        @Test
        void everyMissingField_isReportedInOneMessage() throws Exception {
            CustomException e = rejected(content("{\"name\":\"x\"}"));

            assertEquals(Constants.MANDATORY_FIELDS_MISSING + "courseType, competencies_v6", e.getMessage());
        }

        @Test
        void blankNullAndEmptyValues_countAsMissing() throws Exception {
            CustomException e = rejected(content("{\"courseType\":\"  \",\"competencies_v6\":[]}"));

            assertEquals(Constants.MANDATORY_FIELDS_MISSING + "courseType, competencies_v6", e.getMessage());
            CustomException withNull = rejected(content("{\"courseType\":\"free\",\"competencies_v6\":null}"));
            assertEquals(Constants.MANDATORY_FIELDS_MISSING + "competencies_v6", withNull.getMessage());
        }

        @Test
        void theDescriptionAndLearningOutcome_areNotCheckedHereBecauseTheyGetDefaults() {
            assertDoesNotThrow(() -> validator.rejectMissingMandatoryFields(
                    content("{\"courseType\":\"free\"," + COMPETENCIES + "}")));
        }

        @Test
        void aFieldAddedToTheFile_isCheckedWithoutAnyCodeChange(@TempDir Path directory) throws Exception {
            Path file = directory.resolve("mandatory.json");
            Files.writeString(file, "{\"fields\":[{\"name\":\"description\"},{\"name\":\"topic\",\"onlyWhen\":{\"courseType\":\"free\"}}]}");
            LtiContentFieldsHelper custom = new LtiContentFieldsHelper(mapper, mandatoryPath(file.toUri().toString()));

            CustomException e = assertThrows(CustomException.class,
                    () -> custom.rejectMissingMandatoryFields(content("{\"courseType\":\"free\"}")));
            assertEquals(Constants.MANDATORY_FIELDS_MISSING + "description, topic", e.getMessage());
            assertDoesNotThrow(() -> custom.rejectMissingMandatoryFields(
                    content("{\"courseType\":\"paid\",\"description\":\"d\"}")));
        }

        @Test
        void invalidFile_failsAtStartup(@TempDir Path directory) throws Exception {
            Path noName = directory.resolve("noName.json");
            Files.writeString(noName, "{\"fields\":[{\"onlyWhen\":{}}]}");

            assertThrows(Exception.class, () -> new LtiContentFieldsHelper(mapper, mandatoryPath(noName.toUri().toString())));
            assertThrows(UncheckedIOException.class,
                    () -> new LtiContentFieldsHelper(mapper, mandatoryPath(directory.resolve("missing.json").toUri().toString())));
        }
    }

    @Nested
    class AddDefaultsForMissingFields {


        private static final String IDENTITY = "\"name\":\"Course\",\"externalId\":\"X1\",\"redirectUrl\":\"https://example.org/launch\"";
        private static final String FREE = "\"courseType\":\"free\"";
        private static final String ALL_MANDATORY_WITH_DEFAULTS = """
                {"name":"name","useDefault":true,"value":"n"},{"name":"externalId","useDefault":true,"value":"e"},
                {"name":"redirectUrl","useDefault":true,"value":"https://example.org/r"},
                {"name":"description","useDefault":true,"value":"d"},{"name":"objectives","useDefault":true,"value":"o"},
                {"name":"appIcon","useDefault":true,"value":"https://example.org/i.png"},
                {"name":"courseType","useDefault":true,"value":"paid"},
                {"name":"courseEnrolLimit","useDefault":true,"value":0},
                {"name":"requiredKarmaPoints","useDefault":true,"value":0}
                """;

        private final ObjectMapper mapper = new ObjectMapper();
        private final LtiContentFieldsHelper defaulter = new LtiContentFieldsHelper(mapper, LtiContentOnboardTestProperties.packaged());

        private ObjectNode content(String json) throws Exception {
            return (ObjectNode) mapper.readTree(json);
        }

        private JsonNode partner() throws Exception {
            return mapper.readTree("""
                    {"contentPartnerName":"COURSERA_TESTP","link":"https://example.org/logo.png",
                     "websiteUrl":"https://example.org/site"}
                    """);
        }

        private LtiContentFieldsHelper defaulterWith(Path directory, String fieldsJson) throws Exception {
            Path file = directory.resolve("ltiContentOnboardDefaultValues.json");
            Files.writeString(file, "{\"fields\":[" + fieldsJson + "]}");
            return new LtiContentFieldsHelper(mapper, defaultsPath(file.toUri().toString()));
        }

        @Test
        void contentWithOnlyThePublishFormFields_getsEveryMandatoryTransformField() throws Exception {
            ObjectNode content = content("{" + FREE + "}");

            List<String> added = defaulter.addDefaultsForMissingFields(content, partner());

            assertTrue(added.containsAll(List.of("externalId", "name", "redirectUrl", "description", "objectives",
                    "appIcon", "duration", "topic", "courseEnrolLimit")));
            assertFalse(content.path("externalId").asText().isBlank());
            assertEquals("Course name is not available", content.path("name").asText());
            assertEquals("https://example.org/site", content.path("redirectUrl").asText());
            assertEquals("Description is not available", content.path("description").asText());
            assertEquals("Objectives are not available", content.path("objectives").asText());
            assertEquals("https://example.org/logo.png", content.path("appIcon").asText());
            assertEquals("3600", content.path("duration").asText());
            assertEquals("General", content.path("topic").asText());
            assertEquals(0, content.path("courseEnrolLimit").asInt(-1));
        }

        @Test
        void aMissingCourseId_getsAGeneratedOneThatIsDifferentEachTime() throws Exception {
            ObjectNode first = content("{" + FREE + "}");
            ObjectNode second = content("{" + FREE + "}");

            defaulter.addDefaultsForMissingFields(first, partner());
            defaulter.addDefaultsForMissingFields(second, partner());

            assertNotEquals(first.path("externalId").asText(), second.path("externalId").asText());
        }

        @Test
        void fieldsThatAreNotMandatory_areNeverAdded() throws Exception {
            ObjectNode content = content("{" + IDENTITY + "," + FREE + "}");

            defaulter.addDefaultsForMissingFields(content, partner());

            assertFalse(content.has("summary"));
            assertFalse(content.has("source"));
            assertFalse(content.has("difficultyLevel"));
            assertFalse(content.has("requiredKarmaPoints"));
        }

        @Test
        void courseTypeAndKarmaPoints_arePublishFormFieldsAndAreNeverInvented() throws Exception {
            ObjectNode content = content("{" + IDENTITY + "}");

            CustomException e = assertThrows(CustomException.class,
                    () -> defaulter.addDefaultsForMissingFields(content, partner()));

            assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatusCode());
            assertTrue(e.getMessage().startsWith(Constants.KEY_FIELDS_MISSING));
            assertTrue(e.getMessage().contains("courseType"));
            assertFalse(content.has("courseType"));
        }

        @Test
        void valuesTheContentAlreadyHas_areNeverReplaced() throws Exception {
            ObjectNode content = content("{" + IDENTITY + """
                    ,"description":"Own description","objectives":"Own objectives","appIcon":"https://example.org/own.png",
                     "courseType":"free","courseEnrolLimit":7,"duration":"45","topic":"Own topic"}
                    """);

            List<String> added = defaulter.addDefaultsForMissingFields(content, partner());

            assertTrue(added.isEmpty());
            assertEquals("Own description", content.path("description").asText());
            assertEquals("Own objectives", content.path("objectives").asText());
            assertEquals("https://example.org/own.png", content.path("appIcon").asText());
            assertEquals(7, content.path("courseEnrolLimit").asInt());
            assertEquals("45", content.path("duration").asText());
            assertEquals("Own topic", content.path("topic").asText());
            assertEquals("X1", content.path("externalId").asText());
        }

        @Test
        void blankAndNullValues_countAsMissing() throws Exception {
            ObjectNode content = content("{" + IDENTITY + "," + FREE + ",\"description\":\"  \",\"objectives\":null}");

            List<String> added = defaulter.addDefaultsForMissingFields(content, partner());

            assertTrue(added.containsAll(List.of("description", "objectives")));
            assertEquals("Course", content.path("description").asText());
        }

        @Test
        void borrowing_prefersTheNearestSourceThatHasAValue() throws Exception {
            ObjectNode content = content("{" + IDENTITY + "," + FREE + ",\"description\":\"Own description\"}");

            defaulter.addDefaultsForMissingFields(content, partner());

            assertEquals("Own description", content.path("objectives").asText());
        }

        @Test
        void partnerWithoutALogoOrWebsite_fallsBackToTheValuesInTheFile() throws Exception {
            ObjectNode withoutPartner = content("{" + FREE + "}");
            ObjectNode withoutLogo = content("{" + FREE + "}");

            defaulter.addDefaultsForMissingFields(withoutPartner, null);
            defaulter.addDefaultsForMissingFields(withoutLogo, mapper.readTree("{\"contentPartnerName\":\"X\"}"));

            assertTrue(withoutPartner.path("appIcon").asText().startsWith("https://"));
            assertTrue(withoutPartner.path("redirectUrl").asText().startsWith("https://"));
            assertEquals(withoutPartner.path("appIcon"), withoutLogo.path("appIcon"));
            assertEquals(withoutPartner.path("redirectUrl"), withoutLogo.path("redirectUrl"));
        }

        @Test
        void completedContent_passesTheLiveContentSchema() throws Exception {
            PayloadValidation validation = new PayloadValidation();
            ObjectNode paid = content("{\"courseType\":\"paid\",\"requiredKarmaPoints\":10}");
            ObjectNode free = content("{" + FREE + "}");

            defaulter.addDefaultsForMissingFields(paid, partner());
            defaulter.addDefaultsForMissingFields(free, null);

            assertDoesNotThrow(() -> validation.validatePayload(Constants.CIOS_CONTENT_VALIDATION_FILE_JSON,
                    mapper.createObjectNode().set("content", paid)));
            assertDoesNotThrow(() -> validation.validatePayload(Constants.CIOS_CONTENT_VALIDATION_FILE_JSON,
                    mapper.createObjectNode().set("content", free)));
        }

        @Test
        void everyMandatoryFieldOfTheLiveSchema_isCoveredByADefaultOrAMandatoryRequestField() throws Exception {
            JsonNode live = readResource(Constants.CIOS_CONTENT_VALIDATION_FILE_JSON).path("properties").path("content");
            List<String> mandatory = new ArrayList<>();
            live.path("required").forEach(field -> mandatory.add(field.asText()));
            live.path("then").path("required").forEach(field -> mandatory.add(field.asText()));
            List<String> covered = namesIn("/payloadValidation/ltiContentOnboardDefaultValues.json");
            covered.addAll(namesIn("/payloadValidation/ltiContentOnboardMandatoryFields.json"));

            assertTrue(covered.containsAll(mandatory), "Not covered by either file: " + mandatory);
        }

        @Test
        void everyMandatoryFieldOfTheTransformContentForm_hasADefault() throws Exception {
            List<String> asteriskFields = List.of("name", "description", "duration", "redirectUrl", "appIcon", "externalId",
                    "objectives", "topic");

            assertTrue(namesIn("/payloadValidation/ltiContentOnboardDefaultValues.json").containsAll(asteriskFields),
                    "Without a default: " + asteriskFields);
        }

        @Test
        void aFieldAddedToTheFile_isInjectedWithoutAnyCodeChange(@TempDir Path directory) throws Exception {
            LtiContentFieldsHelper custom = defaulterWith(directory, ALL_MANDATORY_WITH_DEFAULTS + """
                    ,{"name":"summary","useDefault":true,"value":"No summary"},
                     {"name":"difficultyLevel","useDefault":true,"derive":["partner.level"],"value":"Beginner"}
                    """);
            ObjectNode content = content("{" + IDENTITY + "}");

            List<String> added = custom.addDefaultsForMissingFields(content, mapper.readTree("{\"level\":\"Advanced\"}"));

            assertTrue(added.containsAll(List.of("summary", "difficultyLevel")));
            assertEquals("No summary", content.path("summary").asText());
            assertEquals("Advanced", content.path("difficultyLevel").asText());
        }

        @Test
        void aListedFieldTheRequestAlreadyCarries_isLeftAlone(@TempDir Path directory) throws Exception {
            LtiContentFieldsHelper custom = defaulterWith(directory, ALL_MANDATORY_WITH_DEFAULTS
                    + ",{\"name\":\"topic\",\"useDefault\":true,\"value\":\"General\"}");
            ObjectNode content = content("{" + IDENTITY + ",\"topic\":\"Own topic\"}");

            custom.addDefaultsForMissingFields(content, partner());

            assertEquals("Own topic", content.path("topic").asText());
        }

        @Test
        void useDefaultFalse_keepsAMandatoryFieldOutAndTheCallerIsTold(@TempDir Path directory) throws Exception {
            LtiContentFieldsHelper custom = defaulterWith(directory, ALL_MANDATORY_WITH_DEFAULTS
                    .replace("{\"name\":\"courseEnrolLimit\",\"useDefault\":true,\"value\":0}",
                            "{\"name\":\"courseEnrolLimit\",\"useDefault\":false}"));
            ObjectNode content = content("{" + IDENTITY + "}");

            CustomException e = assertThrows(CustomException.class, () -> custom.addDefaultsForMissingFields(content, partner()));

            assertTrue(e.getMessage().endsWith("courseEnrolLimit"));
            assertFalse(content.has("courseEnrolLimit"));
        }

        @Test
        void aMandatoryFieldNotInTheFile_isRejectedInsteadOfInvented(@TempDir Path directory) throws Exception {
            LtiContentFieldsHelper custom = defaulterWith(directory, "");
            ObjectNode content = content("{" + IDENTITY + "," + FREE + "}");

            CustomException e = assertThrows(CustomException.class, () -> custom.addDefaultsForMissingFields(content, partner()));

            assertTrue(e.getMessage().contains("description"));
            assertTrue(e.getMessage().contains("appIcon"));
            assertFalse(e.getMessage().contains("courseType"));
        }

        @Test
        void borrowingDoesNotDependOnTheOrderInTheFile(@TempDir Path directory) throws Exception {
            LtiContentFieldsHelper custom = defaulterWith(directory, ALL_MANDATORY_WITH_DEFAULTS
                    .replace("{\"name\":\"description\",\"useDefault\":true,\"value\":\"d\"},{\"name\":\"objectives\",\"useDefault\":true,\"value\":\"o\"}",
                            "{\"name\":\"objectives\",\"useDefault\":true,\"derive\":[\"content.description\"],\"value\":\"o\"},"
                                    + "{\"name\":\"description\",\"useDefault\":true,\"derive\":[\"content.name\"],\"value\":\"d\"}"));
            ObjectNode content = content("{" + IDENTITY + "}");

            custom.addDefaultsForMissingFields(content, partner());

            assertEquals("Course", content.path("description").asText());
            assertEquals("Course", content.path("objectives").asText());
        }

        @Test
        void invalidFile_failsAtStartup(@TempDir Path directory) {
            assertThrows(Exception.class, () -> defaulterWith(directory,
                    "{\"name\":\"a\",\"useDefault\":true},{\"name\":\"a\",\"useDefault\":false}"));
            assertThrows(Exception.class, () -> defaulterWith(directory, "{\"name\":\"a\"}"));
            Path missing = directory.resolve("nothing.json");
            assertThrows(UncheckedIOException.class, () -> new LtiContentFieldsHelper(mapper, defaultsPath(missing.toUri().toString())));
        }

        private static CbServerProperties defaultsPath(String location) {
            return LtiContentOnboardTestProperties.withDefaultValuesLocation(location);
        }

        private List<String> namesIn(String resource) throws Exception {
            List<String> names = new ArrayList<>();
            readResource(resource).path("fields").forEach(field -> names.add(field.path("name").asText()));
            return names;
        }

        private JsonNode readResource(String path) throws Exception {
            try (InputStream stream = LtiContentFieldsHelperTest.class.getResourceAsStream(path)) {
                return mapper.readTree(stream);
            }
        }
    }

    @Nested
    class AssignPlatformFields {


        private static final Timestamp NOW = Timestamp.valueOf("2026-11-22 10:15:30");

        private final ObjectMapper mapper = new ObjectMapper();
        private final LtiContentFieldsHelper assigner = new LtiContentFieldsHelper(mapper, LtiContentOnboardTestProperties.packaged());

        private ObjectNode content(String json) throws Exception {
            return (ObjectNode) mapper.readTree(json);
        }

        private JsonNode partner() throws Exception {
            return mapper.readTree("""
                    {"id":"partner-id","data":{"contentPartnerName":"COURSERA","link":"https://logo","isActive":true,
                     "websiteUrl":null,"providerTips":["Tip"],"internal":"not copied"}}
                    """);
        }

        private OnboardContentV2Request request() {
            OnboardContentV2Request request = new OnboardContentV2Request();
            request.setPartnerCode("CODE");
            return request;
        }

        @Test
        void stampsStatusDateAndAGeneratedContentId() throws Exception {
            ObjectNode content = content("{\"name\":\"Course\"}");

            assigner.assignPlatformFields(content, request(), partner(), "draft", NOW);

            assertEquals("draft", content.path("status").asText());
            assertEquals(NOW.toString(), content.path("updatedDate").asText());
            assertTrue(content.path("contentId").asText().startsWith("ext_"));
            assertFalse(content.path("accessSettingsEnabled").asBoolean());
        }

        @Test
        void keepsAContentIdTheContentAlreadyHas_andGeneratesDistinctOnesOtherwise() throws Exception {
            ObjectNode withId = content("{\"name\":\"Course\",\"contentId\":\"ext_existing\"}");
            ObjectNode first = content("{\"name\":\"Course\"}");
            ObjectNode second = content("{\"name\":\"Course\"}");

            assigner.assignPlatformFields(withId, request(), partner(), "live", NOW);
            assigner.assignPlatformFields(first, request(), partner(), "live", NOW);
            assigner.assignPlatformFields(second, request(), partner(), "live", NOW);

            assertEquals("ext_existing", withId.path("contentId").asText());
            assertNotEquals(first.path("contentId").asText(), second.path("contentId").asText());
        }

        @Test
        void partnerSummary_carriesIdCodeAndTheDisplayFieldsThePartnerHas() throws Exception {
            ObjectNode content = content("{\"name\":\"Course\"}");

            assigner.assignPlatformFields(content, request(), partner(), "draft", NOW);

            JsonNode summary = content.path("contentPartner");
            assertEquals("partner-id", summary.path("id").asText());
            assertEquals("CODE", summary.path("partnerCode").asText());
            assertEquals("COURSERA", summary.path("contentPartnerName").asText());
            assertEquals("https://logo", summary.path("link").asText());
            assertTrue(summary.path("isActive").asBoolean());
            assertEquals(mapper.readTree("[\"Tip\"]"), summary.path("providerTips"));
            assertFalse(summary.has("websiteUrl"));
            assertFalse(summary.has("internal"));
        }

        @Test
        void searchTags_areTheNameThenTheTagsInLowerCaseWithoutRepeats() throws Exception {
            ObjectNode content = content("{\"name\":\"Data Course\"}");
            OnboardContentV2Request request = request();
            request.setTags(java.util.List.of("Data", "data course", "ESG"));

            assigner.assignPlatformFields(content, request, partner(), "draft", NOW);

            assertEquals(mapper.readTree("[\"data course\",\"data\",\"esg\"]"), content.path("searchTags"));
        }

        @Test
        void contentPartnerSentByTheRequest_isStoredAsSentAndNothingIsAdded() throws Exception {
            ObjectNode content = content("{\"name\":\"Course\"}");
            OnboardContentV2Request request = request();
            request.setContentPartner(mapper.readTree("{\"id\":\"partner-id\",\"partnerCode\":\"CODE\",\"contentPartnerName\":\"Sent name\"}"));

            assigner.assignPlatformFields(content, request, partner(), "draft", NOW);

            assertEquals(mapper.readTree("{\"id\":\"partner-id\",\"partnerCode\":\"CODE\",\"contentPartnerName\":\"Sent name\"}"),
                    content.path("contentPartner"));
        }

        @Test
        void jsonNullContentPartner_countsAsNotSent() throws Exception {
            ObjectNode content = content("{\"name\":\"Course\"}");
            OnboardContentV2Request request = request();
            request.setContentPartner(mapper.nullNode());

            assigner.assignPlatformFields(content, request, partner(), "draft", NOW);

            assertEquals("COURSERA", content.path("contentPartner").path("contentPartnerName").asText());
        }

        @Test
        void providerTips_areAnEmptyListWhenThePartnerHasNone() throws Exception {
            ObjectNode content = content("{\"name\":\"Course\"}");

            assigner.assignPlatformFields(content, request(), mapper.readTree("{\"id\":\"p\",\"data\":{}}"), "draft", NOW);

            assertEquals(mapper.readTree("[]"), content.path("contentPartner").path("providerTips"));
        }

        @Test
        void searchTags_dropBlankTags() throws Exception {
            ObjectNode content = content("{\"name\":\"Course\"}");
            OnboardContentV2Request request = request();
            request.setTags(java.util.List.of("", "  ", "Data"));

            assigner.assignPlatformFields(content, request, partner(), "draft", NOW);

            assertEquals(mapper.readTree("[\"course\",\"data\"]"), content.path("searchTags"));
        }

        @Test
        void searchTags_withoutTagsAreJustTheName() throws Exception {
            ObjectNode content = content("{\"name\":\"Data Course\"}");

            assigner.assignPlatformFields(content, request(), partner(), "draft", NOW);

            assertEquals(mapper.readTree("[\"data course\"]"), content.path("searchTags"));
        }

        @Test
        void accessSettings_followTheRequest() throws Exception {
            ObjectNode off = content("{\"name\":\"Course\"}");
            ObjectNode on = content("{\"name\":\"Course\"}");
            OnboardContentV2Request request = request();
            request.setAccessSettingsEnabled(true);

            assigner.assignPlatformFields(off, request(), partner(), "draft", NOW);
            assigner.assignPlatformFields(on, request, partner(), "draft", NOW);

            assertFalse(off.path("accessSettingsEnabled").asBoolean());
            assertTrue(on.path("accessSettingsEnabled").asBoolean());
        }
    }

}
