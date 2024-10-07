package com.igot.cb.dicussion.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.igot.cb.authentication.util.AccessTokenValidator;
import com.igot.cb.dicussion.entity.DiscussionEntity;
import com.igot.cb.dicussion.repository.DiscussionRepository;
import com.igot.cb.dicussion.service.DiscussionService;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.dto.RespParam;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.PayloadValidation;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DiscussionServiceImpl implements DiscussionService {
    @Autowired
    private PayloadValidation payloadValidation;
    @Autowired
    private DiscussionRepository discussionRepository;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private EsUtilService esUtilService;
    @Autowired
    private CbServerProperties cbServerProperties;
    @Autowired
    private RedisTemplate<String, SearchResult> redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CassandraOperation cassandraOperation;
    @Autowired
    private AccessTokenValidator accessTokenValidator;

    /**
     * Creates a new discussion based on the provided discussion details.
     *
     * @param discussionDetails The details of the discussion to be created.
     * @return A CustomResponse object containing the result of the operation.
     */
    @Override
    public CustomResponse createDiscussion(JsonNode discussionDetails, String token) {
        log.info("DiscussionService::createDiscussion:creating discussion");
        CustomResponse response = new CustomResponse();
        payloadValidation.validatePayload(Constants.DISCUSSION_VALIDATION_FILE, discussionDetails);
        String userId = "19191";//accessTokenValidator.verifyUserToken(token);
        if (StringUtils.isBlank(userId)) {
            response.getParams().setErrmsg(Constants.USER_ID_DOESNT_EXIST);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
        try {
            ((ObjectNode) discussionDetails).put(Constants.CREATED_BY, userId);
            ((ObjectNode) discussionDetails).put(Constants.VOTE_COUNT,0);
            DiscussionEntity jsonNodeEntity = new DiscussionEntity();
            Timestamp currentTime = new Timestamp(System.currentTimeMillis());
            UUID id = UUIDs.timeBased();
            jsonNodeEntity.setDiscussionId(String.valueOf(id));
            jsonNodeEntity.setCreatedOn(currentTime);
            jsonNodeEntity.setData(discussionDetails);
            jsonNodeEntity.setIsActive(true);
            DiscussionEntity saveJsonEntity = discussionRepository.save(jsonNodeEntity);
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode jsonNode = objectMapper.createObjectNode();
            jsonNode.setAll((ObjectNode) saveJsonEntity.getData());
            Map<String, Object> map = objectMapper.convertValue(jsonNode, Map.class);
            esUtilService.addDocument(Constants.DISCUSSION_INDEX, Constants.INDEX_TYPE, String.valueOf(id), map, cbServerProperties.getElasticDiscussionJsonPath());
            cacheService.putCache("discussion_" + String.valueOf(id), jsonNode);
            map.put(Constants.CREATED_ON,currentTime);
            map.put(Constants.DISCUSSION_ID,String.valueOf(id));
            response.setResponseCode(HttpStatus.CREATED);
            response.getParams().setStatus(Constants.SUCCESS);
            response.setResult(map);
        } catch (Exception e) {
            log.error("Failed to create discussion: {}", e.getMessage(), e);
            createErrorResponse(response,Constants.FAILED_TO_CREATE_DISCUSSION,HttpStatus.INTERNAL_SERVER_ERROR,Constants.FAILED);
            return response;
        }
        return response;
    }

    /**
     * Returns the discussion with the given id.
     *
     * @param discussionId The id of the discussion to retrieve
     * @return A CustomResponse containing the discussion's details
     */
    @Override
    public CustomResponse readDiscussion(String discussionId) {
        log.info("reading discussion details");
        CustomResponse response = new CustomResponse();
        if (StringUtils.isEmpty(discussionId)) {
            log.error("discussion not found");
            createErrorResponse(response,Constants.ID_NOT_FOUND,HttpStatus.INTERNAL_SERVER_ERROR,Constants.FAILED);
            return response;
        }
        try {
            String cachedJson = cacheService.getCache(Constants.DISCUSSION_CACHE_PREFIX + discussionId);
            if (StringUtils.isNotEmpty(cachedJson)) {
                log.info("discussion Record coming from redis cache");
                response.setMessage(Constants.SUCCESS);
                response.setResponseCode(HttpStatus.OK);
                response.setResult((Map<String, Object>) objectMapper.readValue(cachedJson, new TypeReference<Object>() {
                }));
            } else {
                Optional<DiscussionEntity> entityOptional = discussionRepository.findById(discussionId);
                if (entityOptional.isPresent()) {
                    DiscussionEntity discussionEntity = entityOptional.get();
                    cacheService.putCache(discussionId, discussionEntity.getData());
                    log.info("discussion Record coming from postgres db");
                    response.setMessage(Constants.SUCCESS);
                    response.setResponseCode(HttpStatus.OK);
                    response.setResult((Map<String, Object>) objectMapper.convertValue(discussionEntity.getData(), new TypeReference<Object>() {
                    }));
                    response.getResult().put(Constants.IS_ACTIVE, discussionEntity.getIsActive());
                    response.getResult().put(Constants.CREATED_ON, discussionEntity.getCreatedOn());
                } else {
                    log.error("Invalid discussionId: {}", discussionId);
                    createErrorResponse(response,Constants.INVALID_ID,HttpStatus.NOT_FOUND,Constants.FAILED);
                    return response;
                }
            }
        } catch (Exception e) {
            log.error(" JSON for discussionId {}: {}", discussionId, e.getMessage(), e);
            createErrorResponse(response,"Failed to read the discussion",HttpStatus.INTERNAL_SERVER_ERROR,Constants.FAILED);
            return response;
        }
        return response;
    }


    /**
     * Updates the discussion with the given id based on the provided update data.
     *
     * @param updateData The data to be used for the update operation.
     * @return A CustomResponse object containing the result of the operation.
     */
    @Override
    public CustomResponse updateDiscussion(JsonNode updateData, String token) {
        CustomResponse response = new CustomResponse();
        try {
            payloadValidation.validatePayload(Constants.DISCUSSION_UPDATE_VALIDATION_FILE, updateData);
            String discussionId = updateData.get(Constants.DISCUSSION_ID).asText();
            Optional<DiscussionEntity> discussionEntity = discussionRepository.findById(discussionId);
            if (discussionEntity.isPresent()) {
                createErrorResponse(response, "Discussion not found", HttpStatus.NOT_FOUND, Constants.FAILED);
                return response;
            }
            DiscussionEntity discussionDbData = discussionEntity.get();
            if (discussionDbData.getIsActive()) {
                createErrorResponse(response, Constants.DISCUSSION_IS_NOT_ACTIVE, HttpStatus.BAD_REQUEST, Constants.FAILED);
                return response;
            }
            JsonNode data = discussionDbData.getData();
            List<String> updateFields = Arrays.asList(Constants.TYPE, Constants.TITLE, Constants.DESCRIPTION_PAYLOAD, Constants.TARGET_TOPIC, Constants.TAG);
            for (String field : updateFields) {
                if (updateData.has(field)) {
                    ((ObjectNode) data).put(field, updateData.get(field).asText());
                }
            }
            if (updateData.has(Constants.ANSWER_POSTS)) {
                String newAnswerPost = updateData.get(Constants.ANSWER_POSTS).get(0).asText();
                if (!StringUtils.isBlank(newAnswerPost)) {
                    Set<String> answerPostSet = new HashSet<>();
                    if (data.has(Constants.ANSWER_POSTS)) {
                        ArrayNode existingAnswerPosts = (ArrayNode) data.get(Constants.ANSWER_POSTS);
                        existingAnswerPosts.forEach(post -> answerPostSet.add(post.asText()));
                    }
                    answerPostSet.add(newAnswerPost);
                    ArrayNode arrayNode = objectMapper.valueToTree(answerPostSet);
                    ((ObjectNode) data).set(Constants.ANSWER_POSTS, arrayNode);
                }
            }
            discussionDbData.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
            discussionDbData.setData(data);
            discussionRepository.save(discussionDbData);
            ObjectNode jsonNode = objectMapper.createObjectNode();
            jsonNode.set(Constants.DISCUSSION_ID, new TextNode(discussionDbData.getDiscussionId()));
            jsonNode.setAll((ObjectNode) discussionDbData.getData());

            Map<String, Object> map = objectMapper.convertValue(jsonNode, Map.class);
            esUtilService.addDocument(Constants.DISCUSSION_INDEX, Constants.INDEX_TYPE, discussionDbData.getDiscussionId(), map, cbServerProperties.getElasticDiscussionJsonPath());
            Map<String, Object> responseMap = objectMapper.convertValue(discussionDbData, new TypeReference<Map<String, Object>>() {
            });
            response.setResponseCode(HttpStatus.OK);
            response.setResult(responseMap);
            response.getParams().setStatus(Constants.SUCCESS);
        } catch (Exception e) {
            log.error("Failed to update the discussion: ", e);
            createErrorResponse(response, "Failed to update the discussion", HttpStatus.INTERNAL_SERVER_ERROR, Constants.FAILED);
            return response;
        }
        return response;
    }


    @Override
    public CustomResponse searchDiscussion(SearchCriteria searchCriteria) {
        log.info("DiscussionServiceImpl::searchDiscussion");
        CustomResponse response = new CustomResponse();
        SearchResult searchResult = redisTemplate.opsForValue().get(generateRedisJwtTokenKey(searchCriteria));
        if (searchResult != null) {
            log.info("DiscussionServiceImpl::searchDiscussion:  search result fetched from redis");
            response.getResult().put(Constants.SEARCH_RESULTS, searchResult);
            createSuccessResponse(response);
            return response;
        }
        String searchString = searchCriteria.getSearchString();
        if (searchString != null && searchString.length() < 2) {
            createErrorResponse(response, Constants.MINIMUM_CHARACTERS_NEEDED, HttpStatus.BAD_REQUEST, Constants.FAILED_CONST);
            return response;
        }
        try {
            searchResult = esUtilService.searchDocuments(Constants.DISCUSSION_INDEX, searchCriteria);
            redisTemplate.opsForValue().set(generateRedisJwtTokenKey(searchCriteria), searchResult, cbServerProperties.getSearchResultRedisTtl(), TimeUnit.SECONDS);
            response.getResult().put(Constants.SEARCH_RESULTS, searchResult);
            createSuccessResponse(response);
            return response;
        } catch (Exception e) {
            createErrorResponse(response, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, Constants.FAILED_CONST);
            redisTemplate.opsForValue().set(generateRedisJwtTokenKey(searchCriteria), searchResult, cbServerProperties.getSearchResultRedisTtl(), TimeUnit.SECONDS);
            return response;
        }
    }

    /**
     * Deletes the discussion with the given id.
     *
     * @param discussionId The id of the discussion to be deleted.
     * @return A CustomResponse object containing the result of the operation.
     */
    @Override
    public CustomResponse deleteDiscussion(String discussionId, String token) {
        log.info("DiscussionServiceImpl::delete Discussion");
        CustomResponse response = new CustomResponse();
        try {
            String userId = accessTokenValidator.verifyUserToken(token);
            if (StringUtils.isBlank(userId)) {
                response.getParams().setErrmsg(Constants.USER_ID_DOESNT_EXIST);
                response.setResponseCode(HttpStatus.BAD_REQUEST);
                return response;
            }

            if (StringUtils.isNotEmpty(discussionId)) {
                Optional<DiscussionEntity> entityOptional = discussionRepository.findById(discussionId);
                if (entityOptional.isPresent()) {
                    DiscussionEntity jasonEntity = entityOptional.get();
                    JsonNode data = jasonEntity.getData();
                    Timestamp currentTime = new Timestamp(System.currentTimeMillis());
                    if (!jasonEntity.getIsActive()) {
                        jasonEntity.setIsActive(false);
                        jasonEntity.setUpdatedOn(currentTime);
                        ((ObjectNode) data).put(Constants.DISCUSSION_ID, discussionId);
                        jasonEntity.setData(data);
                        jasonEntity.setDiscussionId(discussionId);
                        jasonEntity.setUpdatedOn(currentTime);
                        discussionRepository.save(jasonEntity);
                        Map<String, Object> map = objectMapper.convertValue(data, Map.class);
                        map.put(Constants.IS_ACTIVE, false);
                        esUtilService.addDocument(Constants.DISCUSSION_INDEX, Constants.INDEX_TYPE, discussionId, map, cbServerProperties.getElasticDiscussionJsonPath());
                        cacheService.putCache(Constants.DISCUSSION_CACHE_PREFIX + discussionId, data);
                        log.info("Discussion details deleted successfully");
                        response.setResponseCode(HttpStatus.OK);
                        response.setMessage(Constants.DELETED_SUCCESSFULLY);
                        response.getParams().setStatus(Constants.SUCCESS);
                        return response;
                    } else {
                        log.info("Discussion is already inactive.");
                        createErrorResponse(response, Constants.DISCUSSION_IS_INACTIVE, HttpStatus.OK, Constants.SUCCESS);
                        return response;
                    }
                } else {
                    createErrorResponse(response, Constants.INVALID_ID, HttpStatus.BAD_REQUEST, Constants.NO_DATA_FOUND);
                    return response;
                }
            }
        } catch (Exception e) {
            log.error("Error while deleting discussion with ID: {}. Exception: {}", discussionId, e.getMessage(), e);
            createErrorResponse(response, Constants.FAILED_TO_DELETE_DISCUSSION, HttpStatus.INTERNAL_SERVER_ERROR, Constants.FAILED);
            return response;
        }
        return response;
    }

    @Override
    public CustomResponse updateUpVote(Map<String, Object> upVoteData, String token) {
        log.info("DiscussionServiceImpl::updateUpVote");
        CustomResponse response = new CustomResponse();
        String errorMsg = validateUpvoteData(upVoteData);
        if (StringUtils.isNotEmpty(errorMsg)) {
            createErrorResponse(response, errorMsg, HttpStatus.BAD_REQUEST, Constants.FAILED);
            return response;
        }
        try {
            String userId = accessTokenValidator.verifyUserToken(token);
            if (StringUtils.isEmpty(userId)) {
                createErrorResponse(response, Constants.USER_ID_DOESNT_EXIST, HttpStatus.BAD_REQUEST, Constants.FAILED);
                return response;
            }
            Optional<DiscussionEntity> discussionEntity = Optional.of(discussionRepository.findById(upVoteData.get(Constants.DISCUSSION_ID).toString()).get());
            DiscussionEntity discussionDbData = discussionEntity.get();
            HashMap<String, Object> discussionData = objectMapper.convertValue(discussionDbData.getData(), HashMap.class);
            if (!discussionDbData.getIsActive()) {
                createErrorResponse(response, Constants.DISCUSSION_IS_INACTIVE, HttpStatus.BAD_REQUEST, Constants.FAILED);
                return response;
            }
            Object voteCountObj = discussionData.get(Constants.VOTE_COUNT);
            long existingVoteCount = 0L;
            if (voteCountObj instanceof Integer) {
                existingVoteCount = ((Integer) voteCountObj).longValue();
            } else if (voteCountObj instanceof Long) {
                existingVoteCount = (Long) voteCountObj;
            }
            String voteType = (String) upVoteData.get(Constants.VOTETYPE);
            long vote = 0;
            switch (voteType) {
                case Constants.UP:
                    vote = 1L;
                    break;
                case Constants.DOWN:
                    vote = -1L;
                    break;
                default:
                    break;
            }
            Map<String, Object> properties = new HashMap<>();
            properties.put(Constants.DISCUSSION_ID_KEY, upVoteData.get(Constants.DISCUSSION_ID));
            properties.put(Constants.USERID, userId);
            List<Map<String, Object>> existingResponseList = cassandraOperation.getRecordsByPropertiesWithoutFiltering(Constants.KEYSPACE_SUNBIRD, Constants.USER_DISCUSSION_VOTES, properties, null, null);
            if (existingResponseList.isEmpty()) {
                Map<String, Object> propertyMap = new HashMap<>();
                propertyMap.put(Constants.USER_ID_RQST, userId);
                propertyMap.put(Constants.DISCUSSION_ID_KEY, upVoteData.get(Constants.DISCUSSION_ID));
                propertyMap.put(Constants.VOTE_TYPE, voteType);

                ApiResponse result = (ApiResponse) cassandraOperation.insertRecord(Constants.KEYSPACE_SUNBIRD, Constants.USER_DISCUSSION_VOTES, propertyMap);
                Map<String, Object> resultMap = result.getResult();
                if (!resultMap.get(Constants.RESPONSE).equals(Constants.SUCCESS)) {
                    response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return response;
                }
                long newVoteCount = existingVoteCount + vote;
                discussionData.put(Constants.VOTE_COUNT, newVoteCount);
            } else {
                Map<String, Object> userVoteData = existingResponseList.get(0);
                if (userVoteData.get(Constants.VOTE_TYPE).equals(voteType)) {
                    createErrorResponse(response, String.format(Constants.USER_ALREADY_VOTED, voteType), HttpStatus.ALREADY_REPORTED, Constants.FAILED);
                    return response;
                }
                Map<String, Object> updateAttribute = new HashMap<>();
                updateAttribute.put(Constants.VOTE_TYPE, voteType);
                Map<String, Object> compositeKeys = new HashMap<>();
                compositeKeys.put(Constants.USER_ID_RQST, userId);
                compositeKeys.put(Constants.DISCUSSION_ID_KEY, upVoteData.get(Constants.DISCUSSION_ID));
                Map<String, Object> result = cassandraOperation.updateRecordByCompositeKey(Constants.KEYSPACE_SUNBIRD, Constants.USER_DISCUSSION_VOTES, updateAttribute, compositeKeys);
                if (!result.get(Constants.RESPONSE).equals(Constants.SUCCESS)) {
                    createErrorResponse(response, Constants.FAILED_TO_VOTE, HttpStatus.INTERNAL_SERVER_ERROR, Constants.FAILED);
                    return response;
                }
                if (userVoteData.get(Constants.VOTE_TYPE).equals(Constants.UP) && voteType.equals(Constants.DOWN)) {
                    vote = -2L;
                } else {
                    vote = 2L;
                }
            }
            Timestamp currentTime = new Timestamp(System.currentTimeMillis());
            discussionDbData.setUpdatedOn(currentTime);
            JsonNode jsonNode = objectMapper.valueToTree(discussionData);
            discussionDbData.setData(jsonNode);
            discussionRepository.save(discussionDbData);
            esUtilService.addDocument(Constants.DISCUSSION_INDEX, Constants.INDEX_TYPE, discussionDbData.getDiscussionId(), discussionData, cbServerProperties.getElasticDiscussionJsonPath());
            cacheService.putCache(Constants.DISCUSSION_CACHE_PREFIX + discussionDbData.getDiscussionId(), discussionData);
            response.setResponseCode(HttpStatus.OK);
            response.getParams().setStatus(Constants.SUCCESS);
        } catch (Exception e) {
            log.error("Error while updating upvote: {}", e.getMessage(), e);
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        }
        return response;
    }

    public String generateRedisJwtTokenKey(Object requestPayload) {
        if (requestPayload != null) {
            try {
                String reqJsonString = objectMapper.writeValueAsString(requestPayload);
                return JWT.create().withClaim(Constants.REQUEST_PAYLOAD, reqJsonString).sign(Algorithm.HMAC256(Constants.JWT_SECRET_KEY));
            } catch (JsonProcessingException e) {
                log.error("Error occurred while converting json object to json string", e);
            }
        }
        return "";
    }

    public void createSuccessResponse(CustomResponse response) {
        response.setParams(new RespParam());
        response.getParams().setStatus(Constants.SUCCESS);
        response.setResponseCode(HttpStatus.OK);
    }

    public void createErrorResponse(CustomResponse response, String errorMessage, HttpStatus httpStatus, String status) {
        response.setParams(new RespParam());
        response.getParams().setErrmsg(errorMessage);
        response.getParams().setStatus(status);
        response.setResponseCode(httpStatus);
    }

    public String validateUpvoteData(Map<String, Object> upVoteData) {
        StringBuffer str = new StringBuffer();
        List<String> errList = new ArrayList<>();

        if (StringUtils.isBlank((String) upVoteData.get(Constants.DISCUSSION_ID))) {
            errList.add(Constants.DISCUSSION_ID);
        }
        String voteType = (String) upVoteData.get(Constants.VOTETYPE);
        if (StringUtils.isBlank(voteType)) {
            errList.add(Constants.VOTETYPE);
        } else if (!Constants.UP.equalsIgnoreCase(voteType) && !Constants.DOWN.equalsIgnoreCase(voteType)) {
            errList.add("voteType must be either 'up' or 'down'");
        }
        if (!errList.isEmpty()) {
            str.append("Failed Due To Missing Params - ").append(errList).append(".");
        }
        return str.toString();
    }
}
