/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.message.processor;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

import java.util.Set;

public class MessageProcessorUtils {

    /**
     * Since the request stored in the store and consumed by the processor are considered as separate requests
     * we need to remove the existing StatisticsReportingEventHolder object
     *
     * @param messageContext
     */
    public static void removeStatisticsReportingEventHolder(MessageContext messageContext) {

        if (messageContext != null) {
            Set pros = messageContext.getPropertyKeySet();
            if (pros != null) {
                pros.remove(StatisticsConstants.FLOW_STATISTICS_ID);
                pros.remove(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_INDEX);
                pros.remove(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
                pros.remove(StatisticsConstants.MEDIATION_FLOW_STATISTICS_INDEXING_OBJECT);
            }
        }
    }
}
