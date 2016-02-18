/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.aspects.flow.statistics.collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.rest.RESTConstants;

public class ResourceStatisticCollector extends RuntimeStatisticCollector {

    private static final Log log = LogFactory.getLog(ResourceStatisticCollector.class);

    /**
     * Reports statistics for Resource.
     *
     * @param messageContext Current MessageContext of the flow.
     * @param resourceId     Resource Id.
     * @param parentName     parent name.
     * @param isCreateLog    It is statistic flow start or end.
     */
    public static void reportStatisticForResource(MessageContext messageContext, String resourceId, String parentName,
                                                  boolean isCreateLog) {
        if (shouldReportStatistic(messageContext)) {
            String resourceName = getResourceNameForStatistics(messageContext, resourceId);
            if (isCreateLog) {
                createLogForMessageCheckpoint(messageContext, resourceName, ComponentType.RESOURCE, parentName, true,
                                              false, false, true);
            } else {
                if (!messageContext.isResponse()) {
                    boolean isOutOnly =
                            Boolean.parseBoolean(String.valueOf(messageContext.getProperty(SynapseConstants.OUT_ONLY)));
                    if (!isOutOnly) {
                        isOutOnly = (!Boolean.parseBoolean(
                                String.valueOf(messageContext.getProperty(SynapseConstants.SENDING_REQUEST))) &&
                                     !messageContext.isResponse());
                    }
                    if (isOutOnly) {
                        MediatorStatisticCollector
                                .reportStatisticForMessageComponent(messageContext, resourceName, ComponentType.RESOURCE,
                                                           parentName, false, false, false, false);
                    }
                } else {
                    MediatorStatisticCollector
                            .reportStatisticForMessageComponent(messageContext, resourceName, ComponentType.RESOURCE,
                                                                parentName, false, false, false, false);
                }
            }
        }
    }

    private static String getResourceNameForStatistics(MessageContext messageContext, String resourceId) {
        Object synapseRestApi = messageContext.getProperty(RESTConstants.REST_API_CONTEXT);
        Object restUrlPattern = messageContext.getProperty(RESTConstants.REST_URL_PATTERN);
        if (synapseRestApi != null) {
            String textualStringName;
            if (restUrlPattern != null) {
                textualStringName = (String) synapseRestApi + restUrlPattern;
            } else {
                textualStringName = (String) synapseRestApi;
            }
            return textualStringName;
        }
        return resourceId;
    }
}
