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

package org.apache.synapse.analytics;

import com.google.gson.JsonObject;
import org.apache.synapse.analytics.schema.AnalyticsDataSchema;

import java.util.LinkedList;
import java.util.Queue;

public class SimpleAnalyticsService implements AnalyticsService {
    private final Queue<AnalyticsDataSchema> analyticsQueue = new LinkedList<>();
    private boolean enabled = false;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void enableService() {
        enabled = true;
    }

    public void disableService() {
        enabled = false;
    }

    @Override
    public void publish(AnalyticsDataSchema data) {
        analyticsQueue.offer(data);
    }

    public JsonObject fetchAnalytic() {
        if (analyticsQueue.isEmpty()) {
            return null;
        }

        return analyticsQueue.poll().getJsonObject();
    }

    public void clear() {
        analyticsQueue.clear();
    }

    public int getAvailableAnalyticsCount() {
        return analyticsQueue.size();
    }
}
