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
import org.apache.synapse.ServerConfigurationInformation;
import org.apache.synapse.analytics.AnalyticsConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.time.Instant;

public class AnalyticsDataSchema {
    private static String hostname;
    private static String serverName;
    private static String ipAddress;
    private static String publisherId;
    private final Instant timestamp;
    private AnalyticsDataSchemaElement payload;

    public AnalyticsDataSchema(AnalyticsDataSchemaElement payload) {
        this.timestamp = Instant.now();
        this.setPayload(payload);
    }

    public static void setPublisherId(String publisherId) {
        AnalyticsDataSchema.publisherId = publisherId;
    }

    public static void updateServerMetadata(ServerConfigurationInformation serverInfo) {
        hostname = serverInfo.getHostName();
        serverName = serverInfo.getServerName();
        ipAddress = serverInfo.getIpAddress();
        publisherId = SynapsePropertiesLoader.getPropertyValue(
                AnalyticsConstants.SynapseConfiguration.IDENTIFIER, serverInfo.getHostName());
    }

    public void setPayload(AnalyticsDataSchemaElement payload) {
        this.payload = payload;
    }

    public JsonObject getJsonObject() {
        JsonObject exportingAnalytic = new JsonObject();
        JsonObject serverMetadata = new JsonObject();
        serverMetadata.addProperty(AnalyticsConstants.ServerMetadataFieldDef.HOST_NAME, hostname);
        serverMetadata.addProperty(AnalyticsConstants.ServerMetadataFieldDef.SERVER_NAME, serverName);
        serverMetadata.addProperty(AnalyticsConstants.ServerMetadataFieldDef.IP_ADDRESS, ipAddress);
        serverMetadata.addProperty(AnalyticsConstants.ServerMetadataFieldDef.PUBLISHER_ID, publisherId);

        exportingAnalytic.add(AnalyticsConstants.EnvelopDef.SERVER_INFO, serverMetadata);
        exportingAnalytic.addProperty(AnalyticsConstants.EnvelopDef.TIMESTAMP, timestamp.toString());
        exportingAnalytic.addProperty(AnalyticsConstants.EnvelopDef.SCHEMA_VERSION,
                AnalyticsConstants.SynapseConfiguration.SCHEMA_VERSION);
        exportingAnalytic.add(AnalyticsConstants.EnvelopDef.PAYLOAD, payload.toJsonObject());

        return exportingAnalytic;
    }

    public String getJsonString() {
        return getJsonObject().toString();
    }
}
