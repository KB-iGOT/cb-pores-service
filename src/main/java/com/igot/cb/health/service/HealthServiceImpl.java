package com.igot.cb.health.service;


import com.igot.cb.playlist.util.ProjectUtil;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.ApiRespParam;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.Constants;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class HealthServiceImpl implements HealthService {


    @Autowired
    CassandraOperation cassandraOperation;

    @Autowired
    CacheService redisCacheService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EsUtilService esClientService;

    private Logger log = LoggerFactory.getLogger(getClass().getName());

    @Override
    public ApiResponse checkHealthStatus(String requestId) throws Exception {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_HEALTH_CHECK);
        response.getParams().setMsgId(requestId);
        response.getParams().setResMsgId(requestId);
        try {
            response.put(Constants.HEALTHY, true);
            List<Map<String, Object>> healthResults = new ArrayList<>();
            response.put(Constants.CHECKS, healthResults);
            cassandraHealthStatus(response);
            redisHealthStatus(response);
            postgresHealthStatus(response);
            elasticsearchHealthStatus(response);
        } catch (Exception e) {
            log.error("Failed to process health check. Exception: ", e);
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErr(e.getMessage());
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public void cassandraHealthStatus(ApiResponse response) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put(Constants.NAME, Constants.CASSANDRA_DB);
        Boolean res = true;

        try {
            List<Map<String, Object>> cassandraQueryResponse = cassandraOperation.getRecordsByPropertiesByKey(
                    Constants.KEYSPACE_SUNBIRD, Constants.TABLE_SYSTEM_SETTINGS, null,null,null);
            if (cassandraQueryResponse.isEmpty()) {
                res = false;
                setErrorDetails(response, new Exception("Cassandra is unhealthy"));
            }
        } catch (Exception e) {
            res = false;
            setErrorDetails(response, e);
        }
        response.put(Constants.HEALTHY, res);
        result.put(Constants.HEALTHY, res);
        ((List<Map<String, Object>>) response.get(Constants.CHECKS)).add(result);
    }

    private void redisHealthStatus(ApiResponse response) {

        Map<String, Object> result = new HashMap<>();
        result.put(Constants.NAME, Constants.REDIS_CACHE);

        boolean isHealthy = true;

        try{
            isHealthy = redisCacheService.isRedisHealthy();

            if (!isHealthy) {
                setErrorDetails(response, new Exception("Redis is unhealthy"));
            }
        }catch (Exception e) {
            isHealthy = false;
            setErrorDetails(response, e);
        }

        response.put(Constants.HEALTHY, isHealthy);
        result.put(Constants.HEALTHY, isHealthy);
        ((List<Map<String, Object>>) response.get(Constants.CHECKS)).add(result);


    }

    @Transactional(readOnly = true)
    public void postgresHealthStatus(ApiResponse response) {
        Map<String, Object> result = new HashMap<>();
        result.put(Constants.NAME, Constants.POSTGRES_DB);
        Boolean res = true;
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();

        } catch (Exception e) {
            res = false;
            setErrorDetails(response, new Exception("Postgres is unhealthy"));
        }

        response.put(Constants.HEALTHY, res);
        result.put(Constants.HEALTHY, res);
        ((List<Map<String, Object>>) response.get(Constants.CHECKS)).add(result);

    }


    private void elasticsearchHealthStatus(ApiResponse response) {

        Map<String, Object> result = new HashMap<>();
        result.put(Constants.NAME, Constants.ELASTIC_SEARCH);
        boolean isHealthy = true;
        try {
            isHealthy = esClientService.isElasticsearchHealthy();

            if (!isHealthy) {
                response.put(Constants.HEALTHY, false);
                setErrorDetails(response, new Exception("Elasticsearch is unhealthy"));
            }
        }catch (Exception e) {
            isHealthy = false;
            setErrorDetails(response, e);
        }

        response.put(Constants.HEALTHY, isHealthy);
        result.put(Constants.HEALTHY, isHealthy);
        ((List<Map<String, Object>>) response.get(Constants.CHECKS)).add(result);
    }


    private void setErrorDetails(ApiResponse response, Exception e) {

        ApiRespParam params = response.getParams();
        params.setStatus(Constants.FAILED);
        params.setErr(e.getMessage());
        params.setErrMsg(e.getLocalizedMessage());
        response.setParams(params);
    }

}

