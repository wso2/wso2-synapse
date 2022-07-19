/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.analytics.schema;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsDataSchemaElement {
    private final Map<String, Object> attributes = new HashMap<>();

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    private <T> T getAttribute(String key, T defaultValue) {
        if (!attributes.containsKey(key)) {
            return defaultValue;
        }

        try {
            return (T) attributes.get(key);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            if (entry.getValue() instanceof String) {
                json.addProperty(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                json.addProperty(entry.getKey(), (Boolean) entry.getValue());
            } else if (entry.getValue() instanceof Double) {
                json.addProperty(entry.getKey(), (Double) entry.getValue());
            } else if (entry.getValue() instanceof Float) {
                json.addProperty(entry.getKey(), (Float) entry.getValue());
            } else if (entry.getValue() instanceof Long) {
                json.addProperty(entry.getKey(), (Long) entry.getValue());
            } else if (entry.getValue() instanceof Short) {
                json.addProperty(entry.getKey(), (Short) entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                json.addProperty(entry.getKey(), (Integer) entry.getValue());
            } else if (entry.getValue() instanceof JsonObject) {
                json.add(entry.getKey(), (JsonObject) entry.getValue());
            } else if (entry.getValue() instanceof AnalyticsDataSchemaElement) {
                json.add(entry.getKey(), ((AnalyticsDataSchemaElement) entry.getValue()).toJsonObject());
            }
        }

        return json;
    }
}
