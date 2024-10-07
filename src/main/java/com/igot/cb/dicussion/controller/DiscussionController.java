package com.igot.cb.dicussion.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.dicussion.service.DiscussionService;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/discussion")
public class DiscussionController {

    @Autowired
    DiscussionService discussionService;

    @PostMapping("/create")
    public ResponseEntity<CustomResponse> createDiscussion(@RequestBody JsonNode discussionDetails,
                                                           @RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        CustomResponse response = discussionService.createDiscussion(discussionDetails,token);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @GetMapping("/read/{discussionId}")
    public ResponseEntity<CustomResponse> readDiscussion(@PathVariable String discussionId) {
        CustomResponse response = discussionService.readDiscussion(discussionId);
        return new ResponseEntity<>(response,response.getResponseCode());
    }

    @PostMapping("/update")
    public ResponseEntity<CustomResponse> updateDiscussion(@RequestBody JsonNode updateData,
                                                           @RequestHeader(Constants.X_AUTH_TOKEN) String token){
        CustomResponse response = discussionService.updateDiscussion(updateData,token);
        return new ResponseEntity<>(response,response.getResponseCode());
    }

    @PostMapping("/search")
    public ResponseEntity<CustomResponse> searchDiscussion(@RequestBody SearchCriteria searchCriteria){
        CustomResponse response = discussionService.searchDiscussion(searchCriteria);
        return new ResponseEntity<>(response,response.getResponseCode());
    }

    @DeleteMapping("/delete/{discussionId}")
    public ResponseEntity<CustomResponse> deleteDiscussion(@PathVariable String discussionId,
                                                           @RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        CustomResponse response = discussionService.deleteDiscussion(discussionId,token);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/upvote")
    public ResponseEntity<CustomResponse> updateUpVote(@RequestBody Map<String,Object> updateData,
                                                       @RequestHeader(Constants.X_AUTH_TOKEN) String token){
        CustomResponse response = discussionService.updateUpVote(updateData,token);
        return new ResponseEntity<>(response,response.getResponseCode());
    }
}
