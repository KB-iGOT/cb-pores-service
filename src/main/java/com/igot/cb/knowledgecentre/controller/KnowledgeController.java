package com.igot.cb.knowledgecentre.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.knowledgecentre.service.KnowledgeService;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knowledge/centre")
@Slf4j
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/category/create")
    public ResponseEntity<ApiResponse> createCategory(@RequestBody JsonNode dto, @RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response = knowledgeService.createCategory(dto, token);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PutMapping("/category/update/{id}")
    public ResponseEntity<ApiResponse> updateCategory(@RequestBody JsonNode dto, @PathVariable String id,@RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response = knowledgeService.updateCategory(id, dto, token);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PostMapping("/subcategory/create")
    public ResponseEntity<ApiResponse> createSubCategory(@RequestBody JsonNode dto,@RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response = knowledgeService.createSubCategory(dto,token);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PutMapping("/subcategory/update/{id}")
    public ResponseEntity<ApiResponse> updateSubCategory(@RequestBody JsonNode dto, @PathVariable String id, @RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response = knowledgeService.updateSubCategory(id, dto, token);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PostMapping("/article/create")
    public ResponseEntity<ApiResponse> createArticle(@RequestBody JsonNode dto, @RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response = knowledgeService.createArticle(dto, token);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PutMapping("/article/update/{id}")
    public ResponseEntity<ApiResponse> updateArticle(@RequestBody JsonNode dto, @PathVariable String id, @RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response = knowledgeService.updateArticle(id, dto, token);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse> searchEntity(@RequestBody SearchCriteria searchCriteria) {
        ApiResponse response = knowledgeService.searchEntity(searchCriteria);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

}
