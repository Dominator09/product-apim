/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the API Application tier and test the throttling.
 */
public class ChangeApplicationTierAndTestInvokingTestCase extends APIManagerLifecycleBaseTest {
    private APIIdentifier apiIdentifier;
    private static final String APPLICATION_NAME = "ChangeApplicationTierAndTestInvokingTestCase";
    private String applicationNameGold;
    private String applicationNameSilver;

    Map<String, String> requestHeaders;


    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        apiIdentifier = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_1_0_0);

    }


    @Test(groups = {"wso2.am"}, description = "test  invocation of  api under  API tier Gold  and Application Tire Silver.")
    public void testInvokingWithAPIGoldTierApplicationSilver() throws Exception {

        applicationNameSilver = APPLICATION_NAME + TIER_SILVER;
        apiStoreClientUser1.addApplication(applicationNameSilver, TIER_SILVER, "", "");
        //Create publish and subscribe a API
        APIIdentifier apiIdentifier = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        createPublishAndSubscribeToAPI(
                apiIdentifier, API1_CONTEXT, apiPublisherClientUser1, apiStoreClientUser1, applicationNameSilver);


        //get access token
        String accessToken = getAccessToken(apiStoreClientUser1, applicationNameSilver);

        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= SILVER_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_1_0_0 +
                            API1_END_POINT_METHOD, requestHeaders);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API  and Silver Application level tier");
            assertTrue(invokeResponse.getData().contains(API1_RESPONSE_DATA),
                    "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API and Silver Application level tier");

        }

        currentTime = System.currentTimeMillis();


        HttpResponse invokeResponse = HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_1_0_0 +
                API1_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE,
                "Response code mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API level tier");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT),
                "Response data mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API level tier");


    }


    @Test(groups = {"wso2.am"}, description = "test  invocation of  api under API tier Gold  and Application Tire Gold..",
            dependsOnMethods = "testInvokingWithAPIGoldTierApplicationSilver")
    public void testInvokingWithAPIGoldTierApplicationGold() throws Exception {
        applicationNameGold = APPLICATION_NAME + TIER_GOLD;
        apiStoreClientUser1.updateApplication(applicationNameSilver, applicationNameGold, "", "", TIER_GOLD);
        Thread.sleep(THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME);

        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= GOLD_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_1_0_0 +
                            API1_END_POINT_METHOD, requestHeaders);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API  and Gold Application level tier");
            assertTrue(invokeResponse.getData().contains(API1_RESPONSE_DATA),
                    "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API and Gold Application level tier");

        }

        currentTime = System.currentTimeMillis();


        HttpResponse invokeResponse = HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_1_0_0 +
                API1_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE, "Response code mismatched." +
                " Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) + " passed  during :" +
                (currentTime - startTime) + " milliseconds under Gold API level tier and Gold Application level tier");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT), "Response data mismatched. Invocation attempt:"
                + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) + " passed  during :" + (currentTime - startTime) +
                " milliseconds under Gold API level tier " + "and Gold Application level tier");


    }


    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        apiStoreClientUser1.removeApplication(applicationNameSilver);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);

    }


}
