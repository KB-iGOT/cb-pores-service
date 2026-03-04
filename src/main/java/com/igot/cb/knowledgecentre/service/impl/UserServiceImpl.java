package com.igot.cb.knowledgecentre.service.impl;
import com.igot.cb.knowledgecentre.service.UserService;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.igot.cb.pores.exceptions.CustomException;
import org.springframework.http.HttpStatus;


import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final CassandraOperation cassandraOperation;
    private  final ObjectMapper objectMapper;

    @Override
    public List<Object> fetchUserFromPrimary(List<String> userIds) {
        log.info("UserService::fetchUserFromprimary:inside");
        List<Object> userList = new ArrayList<>();
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.ID, userIds);
        List<Map<String, Object>> userInfoList = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                Constants.KEYSPACE_SUNBIRD, Constants.TABLE_USER, propertyMap,
                Arrays.asList(Constants.PROFILE_DETAILS, Constants.FIRST_NAME, Constants.ID, Constants.CHANNEL), null);
        userList = userInfoList.stream()
                .map(userInfo -> {
                    Map<String, Object> userMap = new HashMap<>();
                    String userId = (String) userInfo.get(Constants.ID);
                    String userName = (String) userInfo.get(Constants.FIRST_NAME);
                    userMap.put(Constants.USER_ID_KEY, userId);
                    userMap.put(Constants.FIRST_NAME_KEY, userName);
                    userMap.put(Constants.DEPARTMENT, userInfo.get(Constants.CHANNEL));
                    // Process profile details if present
                    String profileDetails = (String) userInfo.get(Constants.PROFILE_DETAILS);
                    if (StringUtils.isNotBlank(profileDetails)) {
                        try {
                            // Convert JSON profile details to a Map
                            Map<String, Object> profileDetailsMap = objectMapper.readValue(
                                    profileDetails,
                                    new TypeReference<HashMap<String, Object>>() {
                                    });
                            userMap.put(Constants.PROFILE_IMG_KEY, "");
                            userMap.put(Constants.DESIGNATION_KEY, "");
                            userMap.put(Constants.PROFILE_STATUS, "");

                            // Check for profile image and add to userMap if available
                            if (MapUtils.isNotEmpty(profileDetailsMap)) {
                                if (profileDetailsMap.containsKey(Constants.PROFILE_IMG)
                                        && StringUtils.isNotBlank(
                                        (String) profileDetailsMap.get(Constants.PROFILE_IMG))) {
                                    userMap.put(Constants.PROFILE_IMG_KEY,
                                            (String) profileDetailsMap.get(Constants.PROFILE_IMG));
                                }
                                if (profileDetailsMap.containsKey(Constants.PROFESSIONAL_DETAILS)
                                        && ObjectUtils.isNotEmpty(
                                        profileDetailsMap.get(Constants.PROFESSIONAL_DETAILS))) {

                                    Object professionalDetailsObj = profileDetailsMap.get(
                                            Constants.PROFESSIONAL_DETAILS);

                                    if (professionalDetailsObj instanceof List<?>) {
                                        List<?> professionalDetailsList = (List<?>) professionalDetailsObj;

                                        if (!professionalDetailsList.isEmpty()
                                                && professionalDetailsList.get(0) instanceof Map<?, ?>) {
                                            Map<?, ?> firstEntry = (Map<?, ?>) professionalDetailsList.get(
                                                    0);

                                            Object designationObj = firstEntry.get(
                                                    Constants.DESIGNATION);

                                            if (designationObj instanceof String) {
                                                String designation = (String) designationObj;

                                                if (StringUtils.isNotBlank(designation)) {
                                                    userMap.put(Constants.DESIGNATION_KEY, designation);
                                                }
                                            }
                                        }
                                    }
                                }

                                if (profileDetailsMap.containsKey(Constants.PROFILE_STATUS_KEY)
                                        && StringUtils.isNotEmpty(
                                        (String) profileDetailsMap.get(Constants.PROFILE_STATUS_KEY))) {

                                    userMap.put(Constants.PROFILE_STATUS,
                                            (String) profileDetailsMap.get(Constants.PROFILE_STATUS_KEY));
                                }

                            }
                        } catch (Exception e) {
                            log.error("Exception occured while fetching newlyRegistered user's data from cassandra for communityMemberListing:", e);
                            throw new CustomException(Constants.ERROR, "error while processing",
                                    HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }

                    return userMap;
                })
                .collect(Collectors.toList());
        return userList;
    }
}