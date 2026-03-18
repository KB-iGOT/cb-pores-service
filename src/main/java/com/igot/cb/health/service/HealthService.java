package com.igot.cb.health.service;


import com.igot.cb.pores.util.ApiResponse;

public interface HealthService {

    ApiResponse checkHealthStatus(String requestId) throws Exception;

}
