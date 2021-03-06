/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.sampledevice.api.util;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.api.AnalyticsDataAPI;
import org.wso2.carbon.analytics.api.AnalyticsDataAPIUtil;
import org.wso2.carbon.analytics.dataservice.commons.AnalyticsDataResponse;
import org.wso2.carbon.analytics.dataservice.commons.SearchResultEntry;
import org.wso2.carbon.analytics.dataservice.commons.SortByField;
import org.wso2.carbon.analytics.datasource.commons.Record;
import org.wso2.carbon.analytics.datasource.commons.exception.AnalyticsException;
import org.wso2.carbon.apimgt.application.extension.APIManagementProviderService;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.device.mgt.common.authorization.DeviceAccessAuthorizationService;
import org.wso2.carbon.device.mgt.core.service.DeviceManagementProviderService;
import org.wso2.carbon.identity.jwt.client.extension.service.JWTClientManagerService;
import org.wso2.carbon.sampledevice.api.dto.SensorRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides utility functions used by REST-API.
 */
public class APIUtil {

    private static Log log = LogFactory.getLog(APIUtil.class);

    /**
     * @return username of the current user
     */
    public static String getAuthenticatedUser() {
        PrivilegedCarbonContext threadLocalCarbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String username = threadLocalCarbonContext.getUsername();
        String tenantDomain = threadLocalCarbonContext.getTenantDomain();
        if (username.endsWith(tenantDomain)) {
            return username.substring(0, username.lastIndexOf("@"));
        }
        return username;
    }

    /**
     * @return Device management service of current context
     */
    public static DeviceManagementProviderService getDeviceManagementService() {
        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        DeviceManagementProviderService deviceManagementProviderService =
                (DeviceManagementProviderService) ctx.getOSGiService(DeviceManagementProviderService.class, null);
        if (deviceManagementProviderService == null) {
            String msg = "Device Management service has not initialized.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        return deviceManagementProviderService;
    }

    /**
     * @return api management service of current context
     */
    public static APIManagementProviderService getAPIManagementProviderService() {
        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        APIManagementProviderService apiManagementProviderService =
                (APIManagementProviderService) ctx.getOSGiService(APIManagementProviderService.class, null);
        if (apiManagementProviderService == null) {
            String msg = "API management provider service has not initialized.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        return apiManagementProviderService;
    }

    /**
     * @return JWTClient manager of current context
     */
    public static JWTClientManagerService getJWTClientManagerService() {
        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        JWTClientManagerService jwtClientManagerService =
                (JWTClientManagerService) ctx.getOSGiService(JWTClientManagerService.class, null);
        if (jwtClientManagerService == null) {
            String msg = "JWT Client manager service has not initialized.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        return jwtClientManagerService;
    }

    /**
     * @return tenant domain of the user
     */
    public static String getTenantDomainOftheUser() {
        PrivilegedCarbonContext threadLocalCarbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        return threadLocalCarbonContext.getTenantDomain();
    }

    /**
     * @return A Service to authorize device access requests
     */
    public static DeviceAccessAuthorizationService getDeviceAccessAuthorizationService() {
        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        DeviceAccessAuthorizationService deviceAccessAuthorizationService =
                (DeviceAccessAuthorizationService) ctx.getOSGiService(DeviceAccessAuthorizationService.class, null);
        if (deviceAccessAuthorizationService == null) {
            String msg = "Device Authorization service has not initialized.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        return deviceAccessAuthorizationService;
    }

    /**
     * Get sensor data for a device.
     *
     * @param tableName    table name of the data source
     * @param query        query for the table
     * @param sortByFields list of fields to sort the data by
     * @return List of SensorRecords that is sorted by the fields provided
     * @throws AnalyticsException
     */
    public static List<SensorRecord> getAllEventsForDevice(String tableName, String query,
                                                           List<SortByField> sortByFields) throws AnalyticsException {
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        AnalyticsDataAPI analyticsDataAPI = getAnalyticsDataAPI();
        int eventCount = analyticsDataAPI.searchCount(tenantId, tableName, query);
        if (eventCount == 0) {
            return null;
        }
        List<SearchResultEntry> resultEntries = analyticsDataAPI.search(tenantId, tableName, query, 0, eventCount,
                sortByFields);
        List<String> recordIds = getRecordIds(resultEntries);
        AnalyticsDataResponse response = analyticsDataAPI.get(tenantId, tableName, 1, null, recordIds);
        Map<String, SensorRecord> sensorDatas = createSensorData(AnalyticsDataAPIUtil.listRecords(
                analyticsDataAPI, response));
        List<SensorRecord> sortedSensorData = getSortedSensorData(sensorDatas, resultEntries);
        return sortedSensorData;
    }

    /**
     * @param sensorDatas   Map that have the sensor data as key value pairs.
     * @param searchResults List of Search Results that has ids for sensor records.
     * @return List of Sensor Records.
     */
    public static List<SensorRecord> getSortedSensorData(Map<String, SensorRecord> sensorDatas,
                                                         List<SearchResultEntry> searchResults) {
        List<SensorRecord> sortedRecords = new ArrayList<>();
        for (SearchResultEntry searchResultEntry : searchResults) {
            sortedRecords.add(sensorDatas.get(searchResultEntry.getId()));
        }
        return sortedRecords;
    }

    /**
     * @param searchResults List of SearchResultEntry
     * @return List of Ids from SearchResultEntry list
     */
    private static List<String> getRecordIds(List<SearchResultEntry> searchResults) {
        List<String> ids = new ArrayList<>();
        for (SearchResultEntry searchResult : searchResults) {
            ids.add(searchResult.getId());
        }
        return ids;
    }

    /**
     * @param records List of Records that to convert to SensorRecords
     * @return Map of Sensor records
     */
    public static Map<String, SensorRecord> createSensorData(List<Record> records) {
        Map<String, SensorRecord> sensorDatas = new HashMap<>();
        for (Record record : records) {
            SensorRecord sensorData = createSensorData(record);
            sensorDatas.put(sensorData.getId(), sensorData);
        }
        return sensorDatas;
    }

    /**
     * @param record Record to convert to SensorRecord
     * @return SensorRecord
     */
    public static SensorRecord createSensorData(Record record) {
        SensorRecord recordBean = new SensorRecord();
        recordBean.setId(record.getId());
        recordBean.setValues(record.getValues());
        return recordBean;
    }

    /**
     * @return AnalyticthsDataAPI for the context
     */
    public static AnalyticsDataAPI getAnalyticsDataAPI() {
        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        AnalyticsDataAPI analyticsDataAPI =
                (AnalyticsDataAPI) ctx.getOSGiService(AnalyticsDataAPI.class, null);
        if (analyticsDataAPI == null) {
            String msg = "Analytics api service has not initialized.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        return analyticsDataAPI;
    }

    /**
     * @return TenantDomain
     */
    public static String getAuthenticatedUserTenantDomain() {
        PrivilegedCarbonContext threadLocalCarbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        return threadLocalCarbonContext.getTenantDomain();
    }
}
