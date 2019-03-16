/*
 Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.unittest;

import org.json.JSONArray;
import org.json.JSONObject;

import static org.apache.synapse.unittest.Constants.TEST_CASES;
import static org.apache.synapse.unittest.Constants.TEST_CASES_COUNT;

/**
 * Class of the JSON constructor for artifact data.
 */
public class JSONConstructor {

    private JSONObject jsonDataHolder;
    private JSONArray jsonArrayDataHolder;

    /**
     * Initialized JSON object.
     */
    public void initialize() {
        jsonDataHolder = new JSONObject();
    }

    /**
     * Initialized JSON Array.
     */
    void initializeArray() {
        jsonArrayDataHolder = new JSONArray();
    }

    /**
     * Append key-value pairs into the JSON object initiated.
     *
     * @param key   JSON key
     * @param value JSON string value of the key
     */
    void setAttribute(String key, String value) {
        jsonDataHolder.put(key, value);
    }

    /**
     * Append key-value pairs into the JSON Array Object initiated.
     *
     * @param jsonArrayElement as child in the array
     */
    void setAttributeForArray(JSONObject jsonArrayElement) {
        if (jsonArrayDataHolder != null) {
            jsonArrayDataHolder.put(jsonArrayElement);
        }
    }

    /**
     * Append key-value pairs into the JSON object initiated.
     *
     * @param value JSON int value of the key
     */
    void setAttribute(int value) {
        jsonDataHolder.put(TEST_CASES_COUNT, value);
    }

    /**
     * Append key-value pairs into the JSON object initiated.
     *
     * @param value JSON value of the key
     */
    void setAttribute(JSONArray value) {
        jsonDataHolder.put(TEST_CASES, value);
    }

    /**
     * Return JSON object.
     *
     * @return JSONObject
     */
    JSONObject getJSONDataHolder() {
        return this.jsonDataHolder;
    }

    /**
     * Return JSON array.
     *
     * @return JSONArray
     */
    JSONArray getJSONArrayDataHolder() {
        return this.jsonArrayDataHolder;
    }

}
