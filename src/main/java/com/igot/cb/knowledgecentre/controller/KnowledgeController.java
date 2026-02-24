package com.igot.cb.knowledgecentre.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.knowledgecentre.service.KnowledgeService;
import com.igot.cb.knowledgecentre.util.KnowledgeCentreUtil;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knowledge/centre")
@Slf4j
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeCentreUtil knowledgeCentreUtil;

    @PostMapping("/create/{type}")
    public ResponseEntity<ApiResponse> createType(@PathVariable String type, @RequestBody JsonNode dto, @RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response = new ApiResponse();
        switch (type) {
            case Constants.CATEGORY:
                response = knowledgeService.createCategory(dto, token);
                break;
            case Constants.SUBCATEGORY:
                response = knowledgeService.createSubCategory(dto, token);
                break;
            case Constants.ARTICLE:
                response = knowledgeService.createArticle(dto, token);
                break;
            default:
                knowledgeCentreUtil.handleInvalidType(type, response);
                break;
        }
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PutMapping("/update/{type}/{id}")
    public ResponseEntity<ApiResponse> updateType(@PathVariable String type, @PathVariable String id, @RequestBody JsonNode dto, @RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response = new ApiResponse();
        switch (type) {
            case Constants.CATEGORY:
                response = knowledgeService.updateCategory(id, dto, token);
                break;
            case Constants.SUBCATEGORY:
                response = knowledgeService.updateSubCategory(id, dto, token);
                break;
            case Constants.ARTICLE:
                response = knowledgeService.updateArticle(id, dto, token);
                break;
            default:
                knowledgeCentreUtil.handleInvalidType(type, response);
                break;
        }
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PostMapping("/publish/{type}/{id}")
    public ResponseEntity<ApiResponse> publishType(@PathVariable String type, @PathVariable String id, @RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response;
        switch (type) {
            case Constants.CATEGORY:
                response = knowledgeService.publishCategory(id, token);
                break;
            case Constants.SUBCATEGORY:
                response = knowledgeService.publishSubCategory(id, token);
                break;
            case Constants.ARTICLE:
                response = knowledgeService.publishArticle(id, token);
                break;
            default:
                response = new ApiResponse();
                response.getParams().setErrMsg("Invalid type: " + type + ". Supported types are: category, subcategory, article");
                response.setResponseCode(HttpStatus.BAD_REQUEST);
                break;
        }
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @DeleteMapping("/delete/{type}/{id}")
    public ResponseEntity<ApiResponse> deleteType(@PathVariable String type, @PathVariable String id, @RequestHeader(Constants.X_AUTH_TOKEN) String token) {
        ApiResponse response;
        switch (type.toLowerCase()) {
            case Constants.CATEGORY:
                response = knowledgeService.deleteCategory(id, token);
                break;
            case Constants.SUBCATEGORY:
                response = knowledgeService.deleteSubCategory(id, token);
                break;
            case Constants.ARTICLE:
                response = knowledgeService.deleteArticle(id, token);
                break;
            default:
                response = new ApiResponse();
                response.getParams().setErrMsg("Invalid type: " + type + ". Supported types are: category, subcategory, article");
                response.setResponseCode(HttpStatus.BAD_REQUEST);
                break;
        }
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse> searchEntity(@RequestBody SearchCriteria searchCriteria) {
        ApiResponse response = knowledgeService.searchEntity(searchCriteria);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

}
