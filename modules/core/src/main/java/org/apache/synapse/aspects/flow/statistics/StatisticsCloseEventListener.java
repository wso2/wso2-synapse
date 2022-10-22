/*
 * Copyright (c) 2022, WSO2 LLC (http://www.wso2.com).
 *
 * WSO2 LLC licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.aspects.flow.statistics;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;

public class StatisticsCloseEventListener {

    protected String componentName;

    protected ComponentType componentType;

    protected Integer statisticReportingIndex;

    public StatisticsCloseEventListener() {
    }

    public StatisticsCloseEventListener(String componentName, ComponentType componentType,
                                        Integer statisticReportingIndex) {
        this.componentName = componentName;
        this.componentType = componentType;
        this.statisticReportingIndex = statisticReportingIndex;
    }

    /**
     * This method implements how to close the statistic events from synapse mediation engine.
     *
     * @param synCtx synapse message context
     */
    public void invokeCloseEventEntry(MessageContext synCtx) {
        CloseEventCollector.closeFlowForcefully(synCtx, false);
    }
}
