package com.igot.cb.pores.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CbServerPropertiesTest {

    private CbServerProperties cbServerProperties;

    @BeforeEach
    void setUp() {
        cbServerProperties = new CbServerProperties();
        openMocks(this);
    }

    @Test
    void testDefaultContentProperties() {
        // Given
        String expectedValue = "test.default.content.properties";

        // When
        cbServerProperties.setDefaultContentProperties(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getDefaultContentProperties());
    }

    @Test
    void testContentHost() {
        // Given
        String expectedValue = "http://localhost:8080";

        // When
        cbServerProperties.setContentHost(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getContentHost());
    }

    @Test
    void testContentReadEndPoint() {
        // Given
        String expectedValue = "/api/content/read";

        // When
        cbServerProperties.setContentReadEndPoint(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getContentReadEndPoint());
    }

    @Test
    void testContentReadEndPointFields() {
        // Given
        String expectedValue = "id,name,description";

        // When
        cbServerProperties.setContentReadEndPointFields(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getContentReadEndPointFields());
    }

    @Test
    void testRedisInsightIndex() {
        // Given
        int expectedValue = 5;

        // When
        cbServerProperties.setRedisInsightIndex(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getRedisInsightIndex());
    }

    @Test
    void testSearchResultRedisTtl() {
        // Given
        long expectedValue = 3600L;

        // When
        cbServerProperties.setSearchResultRedisTtl(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getSearchResultRedisTtl());
    }

    @Test
    void testSbApiKey() {
        // Given
        String expectedValue = "test-api-key-123";

        // When
        cbServerProperties.setSbApiKey(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getSbApiKey());
    }

    @Test
    void testLearnerServiceUrl() {
        // Given
        String expectedValue = "http://learner-service:8080";

        // When
        cbServerProperties.setLearnerServiceUrl(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getLearnerServiceUrl());
    }

    @Test
    void testOrgSearchPath() {
        // Given
        String expectedValue = "/api/org/search";

        // When
        cbServerProperties.setOrgSearchPath(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getOrgSearchPath());
    }

    @Test
    void testElasticDemandJsonPath() {
        // Given
        String expectedValue = "/config/elastic/demand.json";

        // When
        cbServerProperties.setElasticDemandJsonPath(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getElasticDemandJsonPath());
    }

    @Test
    void testElasticContentJsonPath() {
        // Given
        String expectedValue = "/config/elastic/content.json";

        // When
        cbServerProperties.setElasticContentJsonPath(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getElasticContentJsonPath());
    }

    @Test
    void testElasticInterestJsonPath() {
        // Given
        String expectedValue = "/config/elastic/interest.json";

        // When
        cbServerProperties.setElasticInterestJsonPath(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getElasticInterestJsonPath());
    }

    @Test
    void testElasticBookmarkJsonPath() {
        // Given
        String expectedValue = "/config/elastic/bookmark.json";

        // When
        cbServerProperties.setElasticBookmarkJsonPath(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getElasticBookmarkJsonPath());
    }

    @Test
    void testElasticCiosJsonPath() {
        // Given
        String expectedValue = "/config/elastic/cios.json";

        // When
        cbServerProperties.setElasticCiosJsonPath(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getElasticCiosJsonPath());
    }

    @Test
    void testGetBookmarkDuplicateNotAllowedCategorySingle() {
        // Given
        String categoryValue = "category1";
        ReflectionTestUtils.setField(cbServerProperties, "bookmarkDuplicateNotAllowedCategory", categoryValue);

        // When
        List<String> result = cbServerProperties.getBookmarkDuplicateNotAllowedCategory();

        // Then
        assertEquals(1, result.size());
        assertEquals("category1", result.get(0));
    }

    @Test
    void testGetBookmarkDuplicateNotAllowedCategoryMultiple() {
        // Given
        String categoryValue = "category1,category2,category3";
        ReflectionTestUtils.setField(cbServerProperties, "bookmarkDuplicateNotAllowedCategory", categoryValue);

        // When
        List<String> result = cbServerProperties.getBookmarkDuplicateNotAllowedCategory();

        // Then
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("category1", "category2", "category3"), result);
    }

    @Test
    void testGetBookmarkDuplicateNotAllowedCategoryEmpty() {
        // Given
        String categoryValue = "";
        ReflectionTestUtils.setField(cbServerProperties, "bookmarkDuplicateNotAllowedCategory", categoryValue);

        // When
        List<String> result = cbServerProperties.getBookmarkDuplicateNotAllowedCategory();

        // Then
        assertEquals(1, result.size());
        assertEquals("", result.get(0));
    }

    @Test
    void testSetBookmarkDuplicateNotAllowedCategory() {
        // Given
        String expectedValue = "cat1,cat2,cat3";

        // When
        cbServerProperties.setBookmarkDuplicateNotAllowedCategory(expectedValue);

        // Then
        String actualValue = (String) ReflectionTestUtils.getField(cbServerProperties, "bookmarkDuplicateNotAllowedCategory");
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void testNotifyServiceHost() {
        // Given
        String expectedValue = "http://notify-service:8080";

        // When
        cbServerProperties.setNotifyServiceHost(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getNotifyServiceHost());
    }

    @Test
    void testNotificationAsyncPath() {
        // Given
        String expectedValue = "/api/notification/async";

        // When
        cbServerProperties.setNotificationAsyncPath(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getNotificationAsyncPath());
    }

    @Test
    void testDemandRequestKafkaTopic() {
        // Given
        String expectedValue = "demand-request-topic";

        // When
        cbServerProperties.setDemandRequestKafkaTopic(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getDemandRequestKafkaTopic());
    }

    @Test
    void testSupportEmail() {
        // Given
        String expectedValue = "support@example.com";

        // When
        cbServerProperties.setSupportEmail(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getSupportEmail());
    }

    @Test
    void testDemandRequestTemplate() {
        // Given
        String expectedValue = "demand-request-template";

        // When
        cbServerProperties.setDemandRequestTemplate(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getDemandRequestTemplate());
    }

    @Test
    void testSbUrl() {
        // Given
        String expectedValue = "http://sunbird-service:8080";

        // When
        cbServerProperties.setSbUrl(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getSbUrl());
    }

    @Test
    void testUserSearchEndPoint() {
        // Given
        String expectedValue = "/api/user/search";

        // When
        cbServerProperties.setUserSearchEndPoint(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getUserSearchEndPoint());
    }

    @Test
    void testUserReadEndPoint() {
        // Given
        String expectedValue = "/api/user/read";

        // When
        cbServerProperties.setUserReadEndPoint(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getUserReadEndPoint());
    }

    @Test
    void testAnnouncementDefaultSearchPageSize() {
        // Given
        int expectedValue = 20;

        // When
        cbServerProperties.setAnnouncementDefaultSearchPageSize(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getAnnouncementDefaultSearchPageSize());
    }

    @Test
    void testSbSearchServiceHost() {
        // Given
        String expectedValue = "http://sb-search-service:8080";

        // When
        cbServerProperties.setSbSearchServiceHost(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getSbSearchServiceHost());
    }

    @Test
    void testSbCompositeV4Search() {
        // Given
        String expectedValue = "/api/composite/v4/search";

        // When
        cbServerProperties.setSbCompositeV4Search(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getSbCompositeV4Search());
    }

    @Test
    void testCourseCategoryFacet() {
        // Given
        String[] expectedValue = {"facet1", "facet2", "facet3"};

        // When
        cbServerProperties.setCourseCategoryFacet(expectedValue);

        // Then
        assertArrayEquals(expectedValue, cbServerProperties.getCourseCategoryFacet());
    }

    @Test
    void testElasticDesignationJsonPath() {
        // Given
        String expectedValue = "/config/elastic/designation.json";

        // When
        cbServerProperties.setElasticDesignationJsonPath(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getElasticDesignationJsonPath());
    }

    @Test
    void testOdcsDesignationFramework() {
        // Given
        String expectedValue = "designation-framework";

        // When
        cbServerProperties.setOdcsDesignationFramework(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getOdcsDesignationFramework());
    }

    @Test
    void testOdcsDesignationCategory() {
        // Given
        String expectedValue = "designation-category";

        // When
        cbServerProperties.setOdcsDesignationCategory(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getOdcsDesignationCategory());
    }

    @Test
    void testKnowledgeMS() {
        // Given
        String expectedValue = "http://knowledge-ms:8080";

        // When
        cbServerProperties.setKnowledgeMS(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getKnowledgeMS());
    }

    @Test
    void testOdcsTermCrete() {
        // Given
        String expectedValue = "/api/odcs/term/create";

        // When
        cbServerProperties.setOdcsTermCrete(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getOdcsTermCrete());
    }

    @Test
    void testOdcsDesignationTermRead() {
        // Given
        String expectedValue = "/api/odcs/designation/term/read";

        // When
        cbServerProperties.setOdcsDesignationTermRead(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getOdcsDesignationTermRead());
    }

    @Test
    void testGetOdcsFieldsSingle() {
        // Given
        String fieldsValue = "field1";
        ReflectionTestUtils.setField(cbServerProperties, "odcsFields", fieldsValue);

        // When
        List<String> result = cbServerProperties.getOdcsFields();

        // Then
        assertEquals(1, result.size());
        assertEquals("field1", result.get(0));
    }

    @Test
    void testGetOdcsFieldsMultiple() {
        // Given
        String fieldsValue = "field1,field2,field3";
        ReflectionTestUtils.setField(cbServerProperties, "odcsFields", fieldsValue);

        // When
        List<String> result = cbServerProperties.getOdcsFields();

        // Then
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("field1", "field2", "field3"), result);
    }

    @Test
    void testGetOdcsFieldsEmpty() {
        // Given
        String fieldsValue = "field1,,field3";
        ReflectionTestUtils.setField(cbServerProperties, "odcsFields", fieldsValue);

        // When
        List<String> result = cbServerProperties.getOdcsFields();

        // Then
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("field1", "", "field3"), result);
    }

    @Test
    void testElasticCompJsonPath() {
        // Given
        String expectedValue = "/config/elastic/comp.json";

        // When
        cbServerProperties.setElasticCompJsonPath(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getElasticCompJsonPath());
    }

    @Test
    void testOdcsCompetencyThemeCategory() {
        // Given
        String expectedValue = "competency-theme-category";

        // When
        cbServerProperties.setOdcsCompetencyThemeCategory(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getOdcsCompetencyThemeCategory());
    }

    @Test
    void testOdcsCompetencySubThemeCategory() {
        // Given
        String expectedValue = "competency-sub-theme-category";

        // When
        cbServerProperties.setOdcsCompetencySubThemeCategory(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getOdcsCompetencySubThemeCategory());
    }

    @Test
    void testOdcsFrameworkCreate() {
        // Given
        String expectedValue = "/api/odcs/framework/create";

        // When
        cbServerProperties.setOdcsFrameworkCreate(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getOdcsFrameworkCreate());
    }

    @Test
    void testOdcsFrameworkRead() {
        // Given
        String expectedValue = "/api/odcs/framework/read";

        // When
        cbServerProperties.setOdcsFrameworkRead(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getOdcsFrameworkRead());
    }

    @Test
    void testFrameworkCopy() {
        // Given
        String expectedValue = "/api/framework/copy";

        // When
        cbServerProperties.setFrameworkCopy(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getFrameworkCopy());
    }

    @Test
    void testFrameworkPublish() {
        // Given
        String expectedValue = "/api/framework/publish";

        // When
        cbServerProperties.setFrameworkPublish(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getFrameworkPublish());
    }

    @Test
    void testTopicFrameworkCreate() {
        // Given
        String expectedValue = "framework-create-topic";

        // When
        cbServerProperties.setTopicFrameworkCreate(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getTopicFrameworkCreate());
    }

    @Test
    void testSearchStringMaxRegexLength() {
        // Given
        int expectedValue = 255;

        // When
        cbServerProperties.setSearchStringMaxRegexLength(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getSearchStringMaxRegexLength());
    }

    @Test
    void testCiosContentServiceHost() {
        // Given
        String expectedValue = "http://cios-content-service:8080";

        // When
        cbServerProperties.setCiosContentServiceHost(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getCiosContentServiceHost());
    }

    @Test
    void testCiosContentServiceUpdateApiUrl() {
        // Given
        String expectedValue = "/api/cios/content/update";

        // When
        cbServerProperties.setCiosContentServiceUpdateApiUrl(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getCiosContentServiceUpdateApiUrl());
    }

    @Test
    void testCiosContentServiceSearchApiUrl() {
        // Given
        String expectedValue = "/api/cios/content/search";

        // When
        cbServerProperties.setCiosContentServiceSearchApiUrl(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getCiosContentServiceSearchApiUrl());
    }

    @Test
    void testOrgUpdateEndpoint() {
        // Given
        String expectedValue = "/api/org/update";

        // When
        cbServerProperties.setOrgUpdateEndpoint(expectedValue);

        // Then
        assertEquals(expectedValue, cbServerProperties.getOrgUpdateEndpoint());
    }

    @Test
    void testClassInstantiation() {
        // Given & When
        CbServerProperties properties = new CbServerProperties();

        // Then
        assertNotNull(properties);
    }

    @Test
    void testNullValuesHandling() {
        // Given
        ReflectionTestUtils.setField(cbServerProperties, "bookmarkDuplicateNotAllowedCategory", null);
        ReflectionTestUtils.setField(cbServerProperties, "odcsFields", null);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            cbServerProperties.getBookmarkDuplicateNotAllowedCategory();
        });

        assertThrows(NullPointerException.class, () -> {
            cbServerProperties.getOdcsFields();
        });
    }
}