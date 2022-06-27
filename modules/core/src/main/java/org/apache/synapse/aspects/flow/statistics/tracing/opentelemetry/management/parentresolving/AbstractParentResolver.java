/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.parentresolving;

import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

/**
 * Provides generalization to each type of parent resolver, and provides common methods.
 */
public abstract class AbstractParentResolver {

    protected static boolean isEndpointOrInboundEndpoint(StatisticDataUnit statisticDataUnit) {
        return statisticDataUnit.getComponentType().equals(ComponentType.ENDPOINT) ||
                statisticDataUnit.getComponentType().equals(ComponentType.INBOUNDENDPOINT);
    }

    protected static boolean isFlowContinuableMediator(StatisticDataUnit statisticDataUnit) {
        return statisticDataUnit.isFlowContinuableMediator();
    }

    protected static boolean isForeachMediator(StatisticDataUnit statisticDataUnit) {
        return statisticDataUnit.getComponentType().equals(ComponentType.MEDIATOR) &&
                statisticDataUnit.getComponentName().equalsIgnoreCase("foreachmediator");
    }

    protected static boolean isCallMediator(StatisticDataUnit statisticDataUnit) {
        return ComponentType.MEDIATOR.equals(statisticDataUnit.getComponentType()) &&
                "callmediator".equalsIgnoreCase(statisticDataUnit.getComponentName());
    }

    protected static boolean isSendMediator(StatisticDataUnit statisticDataUnit) {
        return ComponentType.MEDIATOR.equals(statisticDataUnit.getComponentType()) &&
                "sendmediator".equalsIgnoreCase(statisticDataUnit.getComponentName());
    }
}
