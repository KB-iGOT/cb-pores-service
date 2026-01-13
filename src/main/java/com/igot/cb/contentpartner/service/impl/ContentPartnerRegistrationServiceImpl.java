package com.igot.cb.contentpartner.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.authentication.util.AccessTokenValidator;
import com.igot.cb.contentpartner.entity.ContentPartnerRegistrationEntity;
import com.igot.cb.contentpartner.repository.ContentPartnerRegistrationRepository;
import com.igot.cb.contentpartner.service.ContentPartnerRegistrationService;
import com.igot.cb.contentpartner.service.ContentPartnerService;
import com.igot.cb.playlist.util.ProjectUtil;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.PayloadValidation;
import com.igot.cb.producer.Producer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

@Service
@Slf4j
public class ContentPartnerRegistrationServiceImpl implements ContentPartnerRegistrationService {
    private final PayloadValidation payloadValidation;
    private final ContentPartnerRegistrationRepository registrationRepository;
    private final ObjectMapper objectMapper;
    private final CbServerProperties cbServerProperties;
    private final EsUtilService esUtilService;
    private final AccessTokenValidator accessTokenValidator;
    private final ContentPartnerService contentPartnerService;

    @Autowired
    private Producer kafkaProducer;

    public ContentPartnerRegistrationServiceImpl(
            PayloadValidation payloadValidation,
            ContentPartnerRegistrationRepository registrationRepository,
            ObjectMapper objectMapper,
            CbServerProperties cbServerProperties,
            EsUtilService esUtilService,
            AccessTokenValidator accessTokenValidator,
            ContentPartnerService contentPartnerService
    ) {
        this.payloadValidation = payloadValidation;
        this.registrationRepository = registrationRepository;
        this.objectMapper = objectMapper;
        this.cbServerProperties = cbServerProperties;
        this.esUtilService = esUtilService;
        this.accessTokenValidator=accessTokenValidator;
        this.contentPartnerService = contentPartnerService;
    }

    @Override
    public ApiResponse insert(JsonNode registrationDetails) {
        log.info("ContentPartnerRegistrationServiceImpl::createContentPartnerRegistration");
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_PARTNER_CREATE);
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        payloadValidation.validatePayload(Constants.PAYLOAD_VALIDATION_FILE_CONTENT_PARTNER_REGISTRATION, registrationDetails);
        String organizationName = registrationDetails.path("contentPartnerName").asText("");
        String contactName = registrationDetails.path(Constants.EVENT_CONTACT_NAME).asText("");
        String email = registrationDetails.path("email").asText("");

        Optional<ContentPartnerRegistrationEntity> existingByOrgName =
                registrationRepository.findByContentPartnerOrganizationName(organizationName);

        if (existingByOrgName.isPresent()) {
            ProjectUtil.errorResponse(response, "Organization Name already registered", HttpStatus.BAD_REQUEST);
            return response;
        }
        Optional<ContentPartnerRegistrationEntity> existingByEmail =
                registrationRepository.findByContentPartnerEmail(email);
        if (existingByEmail.isPresent()) {
            ProjectUtil.errorResponse(response, "Email already registered", HttpStatus.BAD_REQUEST);
            return response;
        }
        String id = UUID.randomUUID().toString();
        String applicationId;
        do {
            applicationId = Constants.APPLICATION_ID_PREFIX +
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 5)
                            .toUpperCase();
        } while (registrationRepository.existsByApplicationId(applicationId));

        ObjectNode jsonNode = (ObjectNode) registrationDetails;
        jsonNode.put(Constants.ID, id);
        jsonNode.put(Constants.CREATED_ON, currentTime.toString());
        jsonNode.put(Constants.UPDATED_ON, currentTime.toString());
        jsonNode.put(Constants.STATUS, Constants.PENDING);
        jsonNode.put(Constants.APPLICATION_ID, applicationId);
        addSearchTags(registrationDetails);
        ContentPartnerRegistrationEntity entity = new ContentPartnerRegistrationEntity();
        entity.setId(id);
        entity.setData(registrationDetails);
        entity.setCreatedOn(currentTime);
        entity.setUpdatedOn(currentTime);
        entity.setApplicationId(applicationId);
        ContentPartnerRegistrationEntity savedEntity = registrationRepository.save(entity);

        Map<String, Object> map = objectMapper.convertValue(savedEntity.getData(), Map.class);
        esUtilService.addDocument(Constants.CONTENT_PARTNER_REGISTRATION_INDEX_NAME, Constants.INDEX_TYPE, id, map, cbServerProperties.getElasticContentPartnerJsonPath());
        Map<String, Object> result = objectMapper.convertValue(savedEntity, Map.class);
        // send mail to content partner about successful registration
        Map<String, Object> event = new HashMap<>();
        event.put(Constants.EVENT_STATUS, Constants.PENDING);
        event.put(Constants.EVENT_EMAIL, email);
        event.put(Constants.EVENT_PARTNER_NAME, organizationName);
        event.put(Constants.EVENT_REGISTRATION_ID, applicationId);
        event.put(Constants.EVENT_CONTACT_NAME,contactName);
        event.put(Constants.COMMENT,"");
        kafkaProducer.push(cbServerProperties.getContentPartnerRegistrationTopic(), event);

        log.info("Content Partner Registration Created Successfully");
        response.setResult(result);
        return response;
    }

    @Override
    public ApiResponse update(JsonNode partnerDetails, String token) {
        log.info("ContentPartnerRegistrationServiceImpl::updateContentPartnerRegistration");
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_PARTNER_UPDATE);
        String userId = accessTokenValidator.verifyUserToken(token);
        if(userId.equalsIgnoreCase(Constants.UNAUTHORIZED)){
            ProjectUtil.errorResponse(response, Constants.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
            return response;
        }
        String existingId = partnerDetails.path(Constants.ID).asText(null);
        String newStatus = partnerDetails.path(Constants.STATUS).asText(null);
        String comment = partnerDetails.path(Constants.COMMENT).asText("");
        if (StringUtils.isBlank(existingId) || StringUtils.isBlank(newStatus)) {
            ProjectUtil.errorResponse(response, "id and status are required", HttpStatus.BAD_REQUEST);
            return response;
        }
        if (!Constants.APPROVED.equalsIgnoreCase(newStatus) &&
                !Constants.REJECTED.equalsIgnoreCase(newStatus)) {
            ProjectUtil.errorResponse(response, "Invalid status. Allowed values: APPROVED, REJECTED", HttpStatus.BAD_REQUEST);
            return response;
        }
        if (Constants.REJECTED.equalsIgnoreCase(newStatus) && StringUtils.isBlank(comment)) {
            ProjectUtil.errorResponse(response, Constants.ERR_COMMENT_REQUIRED, HttpStatus.BAD_REQUEST);
            return response;
        }
        Optional<ContentPartnerRegistrationEntity> content = registrationRepository.findById(existingId);
        if (content.isEmpty()) {
            ProjectUtil.errorResponse(response, "Content Partner Registration not found", HttpStatus.NOT_FOUND);
            return response;
        }

        ContentPartnerRegistrationEntity entity = content.get();
        ObjectNode dataNode = (ObjectNode) entity.getData();
        String email = dataNode.path("email").asText("");
        String organizationName = dataNode.path("contentPartnerName").asText("");
        String contactName = dataNode.path(Constants.EVENT_CONTACT_NAME).asText("");
        String applicationId = dataNode.path(Constants.APPLICATION_ID).asText("");
        dataNode.put(Constants.STATUS, newStatus);
        dataNode.put(Constants.COMMENT, comment);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        entity.setUpdatedOn(now);
        dataNode.put(Constants.UPDATED_ON, now.toString());
        dataNode.put(Constants.ID, existingId);
        ContentPartnerRegistrationEntity updated = registrationRepository.save(entity);
        saveContentPartnerIfApproved(updated, newStatus);
        Map<String, Object> esMap = objectMapper.convertValue(updated.getData(), Map.class);
        esUtilService.updateDocument(
                Constants.CONTENT_PARTNER_REGISTRATION_INDEX_NAME,
                Constants.INDEX_TYPE,
                existingId,
                esMap,
                cbServerProperties.getElasticContentPartnerJsonPath()
        );

        Map<String, Object> resultMap = objectMapper.convertValue(updated, Map.class);
        Map<String, Object> event = new HashMap<>();
        event.put(Constants.EVENT_STATUS, newStatus);
        event.put(Constants.EVENT_EMAIL, email);
        event.put(Constants.EVENT_PARTNER_NAME, organizationName);
        event.put(Constants.EVENT_REGISTRATION_ID, applicationId);
        event.put(Constants.EVENT_CONTACT_NAME,contactName);
        event.put(Constants.COMMENT,comment);
        log.info("event",event);
        kafkaProducer.push(cbServerProperties.getContentPartnerRegistrationTopic(), event);

        response.setResult(resultMap);
        return response;
    }

    private void saveContentPartnerIfApproved(ContentPartnerRegistrationEntity registrationEntity, String newStatus) {
        if (!Constants.APPROVED.equalsIgnoreCase(newStatus)) {
            return;
        }
        try {
            ObjectNode registrationData = registrationEntity.getData().deepCopy();
            registrationData.remove(List.of(Constants.CREATED_ON, Constants.UPDATED_ON, Constants.STATUS, Constants.EMAIL, Constants.PHONE_NUMBER, Constants.CONTACT_NAME, Constants.APPLICATION_ID, Constants.COMMENT, Constants.SEARCHTAGS));
            log.info(Constants.CONTENT_PARTNER_CREATE_START, registrationEntity.getId());
            ApiResponse createResponse = contentPartnerService.createContentPartner(registrationData);
            if (HttpStatus.OK.equals(createResponse.getResponseCode())) {
                log.info(Constants.CONTENT_PARTNER_CREATE_SUCCESS, registrationEntity.getId());
            } else {
                log.error(Constants.CONTENT_PARTNER_CREATE_FAILED, createResponse.getParams().getErrMsg());
            }
        } catch (Exception e) {
            log.error(Constants.CONTENT_PARTNER_CREATE_EXCEPTION, e);
        }
    }

    @Override
    public ApiResponse read(String applicationId, String email) {
        log.info("ContentPartnerRegistrationServiceImpl::read");
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_PARTNER_READ);
        if (StringUtils.isAllEmpty(applicationId, email)) {
            ProjectUtil.errorResponse(response, Constants.ERR_ID_OR_EMAIL_REQUIRED, HttpStatus.BAD_REQUEST);
            return response;
        }
        try {
            Optional<ContentPartnerRegistrationEntity> entityOptional = Optional.empty();
            if (StringUtils.isNotEmpty(applicationId) && StringUtils.isNotEmpty(email)) {
                String fetchedId = fetchIdFromElasticsearch(email, applicationId);
                if (StringUtils.isNotBlank(fetchedId)) {
                    entityOptional = registrationRepository.findById(fetchedId);
                }
                if (entityOptional.isEmpty()) {
                    ProjectUtil.errorResponse(response, Constants.INVALID_ID_OR_EMAIL, HttpStatus.BAD_REQUEST);
                    return response;
                }
            }
            response.setResult(objectMapper.convertValue(entityOptional.get(), Map.class));
        } catch (Exception e) {
            log.error("Error while reading content partner", e);
            ProjectUtil.errorResponse(response, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    private String fetchIdFromElasticsearch(String email, String applicationId) {
        try {
            log.info("Fetching ID from Elasticsearch for email: {} and applicationId: {}", email, applicationId);
            SearchCriteria searchCriteria = new SearchCriteria();
            HashMap<String, Object> filterCriteriaMap = new HashMap<>();
            filterCriteriaMap.put(Constants.EMAIL, email);
            filterCriteriaMap.put(Constants.APPLICATION_ID, applicationId);
            searchCriteria.setFilterCriteriaMap(filterCriteriaMap);
            searchCriteria.setRequestedFields(Arrays.asList(Constants.ID));
            SearchResult searchResult = esUtilService.searchDocuments(Constants.CONTENT_PARTNER_REGISTRATION_INDEX_NAME, searchCriteria);
            if (searchResult != null && searchResult.getData() != null) {
                JsonNode dataNode = searchResult.getData();
                if (dataNode.isArray() && dataNode.size() > 0) {
                    JsonNode firstResult = dataNode.get(0);
                    if (firstResult.has(Constants.ID)) {
                        String fetchedId = firstResult.get(Constants.ID).asText();
                        log.info(Constants.ES_ID_FOUND_FOR_EMAIL_AND_APP_ID, email, applicationId, fetchedId);
                        return fetchedId;
                    }
                }
            }
            log.warn(Constants.ES_NO_RECORD_FOR_EMAIL_AND_APP_ID, email, applicationId);
            return null;
        } catch (Exception e) {
            log.error(Constants.ES_ERROR_FETCHING_ID_FOR_EMAIL_AND_APP_ID, email, applicationId, e);
            return null;
        }
    }

    @Override
    public ApiResponse readById(String id, String token) {
        log.info("ContentPartnerRegistrationServiceImpl::readById");
        String userId = accessTokenValidator.verifyUserToken(token);
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_PARTNER_READ);
        if (userId.equalsIgnoreCase(Constants.UNAUTHORIZED)) {
            ProjectUtil.errorResponse(response, Constants.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
            return response;
        }
        if (StringUtils.isAllEmpty(id)) {
            ProjectUtil.errorResponse(response, Constants.ID_MISSING, HttpStatus.BAD_REQUEST);
            return response;
        }
        try {
            Optional<ContentPartnerRegistrationEntity> entityOptional = registrationRepository.findById(id);
            if (entityOptional.isEmpty()) {
                ProjectUtil.errorResponse(response, Constants.ID_NOT_FOUND, HttpStatus.NOT_FOUND);
                return response;
            }
            response.setResult(objectMapper.convertValue(entityOptional.get(), Map.class));
        } catch (Exception e) {
            log.error("Error while reading content partner", e);
            ProjectUtil.errorResponse(response, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Override
    public ApiResponse searchEntity(SearchCriteria searchCriteria,String token) {
        log.info("ContentPartnerRegistrationServiceImpl::searchEntity:searching the content partner");
        String searchString = searchCriteria.getSearchString();
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_PARTNER_SEARCH);
        String userId = accessTokenValidator.verifyUserToken(token);
        if(userId.equalsIgnoreCase(Constants.UNAUTHORIZED)){
            ProjectUtil.errorResponse(response, Constants.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
            return response;
        }
        if (searchString != null && searchString.length() < 3) {
            ProjectUtil.errorResponse(response, "Minimum 3 characters are required to search", HttpStatus.BAD_REQUEST);
            return response;
        }
        try {
            SearchResult searchResult =
                    esUtilService.searchDocuments(Constants.CONTENT_PARTNER_REGISTRATION_INDEX_NAME, searchCriteria);
            Map<String, Object> jsonMap =
                    objectMapper.convertValue(searchResult, new TypeReference<Map<String, Object>>() {
                    });
            response.setResult(jsonMap);
            return response;
        } catch (Exception e) {
            log.error("Error while processing to search", e);
            ProjectUtil.errorResponse(response, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    private JsonNode addSearchTags(JsonNode formattedData) {
        List<String> searchTags = new ArrayList<>();

        // Preserve existing searchTags if present
        if (formattedData.has("searchTags") && formattedData.get("searchTags").isArray()) {
            ArrayNode existingSearchTags = (ArrayNode) formattedData.get("searchTags");
            existingSearchTags.forEach(tag -> {
                if (tag.isTextual() && !tag.asText().isEmpty()) {
                    searchTags.add(tag.asText());
                }
            });
        }

        if (formattedData.has("contentPartnerName")) {
            String partnerName = formattedData.get("contentPartnerName").textValue();
            if (StringUtils.isNotBlank(partnerName)) {
                if (!searchTags.contains(partnerName.toLowerCase())) {
                    searchTags.add(partnerName.toLowerCase());
                }
            }
        }

        ArrayNode searchTagsArray = objectMapper.valueToTree(searchTags);
        ((ObjectNode) formattedData).put("searchTags", searchTagsArray);
        return formattedData;
    }


}