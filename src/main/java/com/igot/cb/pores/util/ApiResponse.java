package com.igot.cb.pores.util;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;

/**
 * @author Mahesh RV
 * @author Ruksana
 */
public class ApiResponse {
    private String id;
    private String ver;
    private String ts;
    private ApiRespParam params;
    private HttpStatus responseCode;

    private transient Map<String, Object> result = new HashMap<>();

    public ApiResponse() {
        this.ver = "v1";
        this.ts = new Timestamp(System.currentTimeMillis()).toString();
        this.params = new ApiRespParam(UUID.randomUUID().toString());
    }

    public ApiResponse(String id) {
        this();
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVer() {
        return ver;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public ApiRespParam getParams() {
        return params;
    }

    public void setParams(ApiRespParam params) {
        this.params = params;
    }

    public HttpStatus getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(HttpStatus responseCode) {
        this.responseCode = responseCode;
    }

    public Map<String, Object> getResult() {
        return this.result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    public Object get(String key) {
        return result.get(key);
    }

    public void put(String key, Object vo) {
        result.put(key, vo);
    }

    public void putAll(Map<String, Object> map) {
        result.putAll(map);
    }

    public boolean containsKey(String key) {
        return result.containsKey(key);
    }

}
