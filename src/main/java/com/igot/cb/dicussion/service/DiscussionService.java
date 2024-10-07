package com.igot.cb.dicussion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;

import java.util.Map;

public interface DiscussionService {
    CustomResponse createDiscussion(JsonNode discussionDetails, String token);

    CustomResponse readDiscussion(String discussionId);

    CustomResponse updateDiscussion(JsonNode updateData,String token);

    CustomResponse searchDiscussion(SearchCriteria searchCriteria);

    CustomResponse deleteDiscussion(String discussionId,String token);

    CustomResponse updateUpVote(Map<String,Object> upVoteData,String token);
}
