package com.igot.cb.contentpartner.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.contentpartner.entity.ContentPartnerRegistrationEntity;
import com.igot.cb.contentpartner.repository.ContentPartnerRegistrationRepository;
import com.igot.cb.contentpartner.service.ContentPartnerRegistrationService;
import com.igot.cb.playlist.util.ProjectUtil;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.PayloadValidation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ContentPartnerRegistrationServiceImpl implements ContentPartnerRegistrationService {
    @Autowired
    private PayloadValidation payloadValidation;
    @Autowired
    private ContentPartnerRegistrationRepository registrationRepository;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CbServerProperties cbServerProperties;
    @Autowired
    private EsUtilService esUtilService;


    @Override
    public ApiResponse createOrUpdate(JsonNode partnerDetails) {
        log.info("ContentPartnerServiceImpl::createOrUpdate:inside");
        ApiResponse response = new ApiResponse();
        try {
            if (partnerDetails.get(Constants.ID) == null) {
                response = createContentPartnerRegistration(partnerDetails);
            } else {
                response = updateContentPartner(partnerDetails);
            }
            return response;
        } catch (Exception e) {
            response.getParams().setErrMsg(e.getMessage());
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        }
    }

    private ApiResponse createContentPartnerRegistration(JsonNode registrationDetails) {
        log.info("ContentPartnerRegistrationServiceImpl::createContentPartnerRegistration");

        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_PARTNER_CREATE);
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        payloadValidation.validatePayload(Constants.PAYLOAD_VALIDATION_FILE_CONTENT_PARTNER_REGISTRATION, registrationDetails);
        String organizationName = registrationDetails.path("contentPartnerName").asText("");
        String email = registrationDetails.path("email").asText("");

        Optional<ContentPartnerRegistrationEntity> existingByOrgName =
                registrationRepository.findByContentPartnerOrganizationName(organizationName);

        if (existingByOrgName.isPresent()) {
            response.getParams().setErrMsg("Organization Name already registered");
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }

        Optional<ContentPartnerRegistrationEntity> existingByEmail =
                registrationRepository.findByContentPartnerEmail(email);

        if (existingByEmail.isPresent()) {
            response.getParams().setErrMsg("Email already registered");
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }

        String id = UUID.randomUUID().toString();

        ((ObjectNode) registrationDetails).put("id", id);
        ((ObjectNode) registrationDetails).put("createdOn", String.valueOf(currentTime));
        ((ObjectNode) registrationDetails).put("updatedOn", String.valueOf(currentTime));
        ((ObjectNode) registrationDetails).put("status", Constants.PENDING);
        ContentPartnerRegistrationEntity entity = new ContentPartnerRegistrationEntity();
        entity.setId(id);
        entity.setData(registrationDetails);
        entity.setCreatedOn(currentTime);
        entity.setUpdatedOn(currentTime);
        ContentPartnerRegistrationEntity savedEntity = registrationRepository.save(entity);

        Map<String, Object> map = objectMapper.convertValue(savedEntity.getData(), Map.class);
        esUtilService.addDocument(Constants.CONTENT_PARTNER_REGISTRATION_INDEX_NAME, Constants.INDEX_TYPE, id, map, cbServerProperties.getElasticContentPartnerJsonPath());
        Map<String, Object> result = objectMapper.convertValue(savedEntity, Map.class);
        cacheService.putCache(savedEntity.getId(), result);

        log.info("Content Partner Registration Created Successfully");

        response.setResult(result);
        response.setResponseCode(HttpStatus.OK);
        return response;
    }

    private ApiResponse updateContentPartner(JsonNode partnerDetails) {
        log.info("ContentPartnerRegistrationServiceImpl::updateContentPartnerRegistration");

        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_PARTNER_UPDATE);
        String existingId = partnerDetails.path("id").asText(null);
        String newStatus = partnerDetails.path("status").asText(null);

        if (existingId == null || newStatus == null) {
            response.getParams().setErrMsg("id and status are required");
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
        if (!Constants.APPROVED.equalsIgnoreCase(newStatus) &&
                !Constants.REJECTED.equalsIgnoreCase(newStatus)) {
            response.getParams().setErrMsg("Invalid status. Allowed values: APPROVED, REJECTED");
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
        Optional<ContentPartnerRegistrationEntity> content = registrationRepository.findById(existingId);
        if (content.isEmpty()) {
            response.getParams().setErrMsg("Content Partner Registration not found");
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.NOT_FOUND);
            return response;
        }

        ContentPartnerRegistrationEntity entity = content.get();
        ObjectNode dataNode = (ObjectNode) entity.getData();
        dataNode.put("status", newStatus);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        entity.setUpdatedOn(now);
        dataNode.put("updatedOn", now.toString());
        ContentPartnerRegistrationEntity updated = registrationRepository.save(entity);

        Map<String, Object> esMap = objectMapper.convertValue(updated.getData(), Map.class);
        esUtilService.updateDocument(
                Constants.CONTENT_PARTNER_REGISTRATION_INDEX_NAME,
                Constants.INDEX_TYPE,
                existingId,
                esMap,
                cbServerProperties.getElasticContentPartnerJsonPath()
        );

        Map<String, Object> resultMap = objectMapper.convertValue(updated, Map.class);
        cacheService.putCache(updated.getId(), resultMap);

        response.setResult(resultMap);
        response.setResponseCode(HttpStatus.OK);
        return response;
    }

    @Override
    public ApiResponse read(String id) {
        log.info("ContentPartnerRegistrationServiceImpl::read:reading information about the content partner");
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_PARTNER_READ);
        if (StringUtils.isEmpty(id)) {
            response.getParams().setErrMsg(Constants.ID_NOT_FOUND);
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        }
        try {
            String cachedJson = cacheService.getCache(id);
            if (StringUtils.isNotEmpty(cachedJson)) {
                log.info("Record coming from redis cache");
                response.setResponseCode(HttpStatus.OK);
                response.setResult(objectMapper.readValue(cachedJson, new TypeReference<Map>() {
                }));
            } else {
                Optional<ContentPartnerRegistrationEntity> entityOptional = registrationRepository.findById(id);
                if (entityOptional.isPresent()) {
                    ContentPartnerRegistrationEntity entity = entityOptional.get();
                    cacheService.putCache(id, entity);
                    log.info("Record coming from postgres db");
                    response.setResponseCode(HttpStatus.OK);
                    response.setResult(objectMapper.convertValue(entity, Map.class));
                } else {
                    response.getParams().setErrMsg(Constants.INVALID_ID);
                    response.getParams().setStatus(Constants.FAILED);
                    response.setResponseCode(HttpStatus.BAD_REQUEST);
                }
            }
        } catch (Exception e) {
            log.error("error while processing", e);
            response.getParams().setErrMsg(e.getMessage());
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }


}