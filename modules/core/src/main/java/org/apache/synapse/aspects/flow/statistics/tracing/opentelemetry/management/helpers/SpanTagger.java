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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.helpers;

import io.opentelemetry.api.trace.Span;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.OpenTelemetryManagerHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models.SpanWrapper;

/**
 * Applies tags to Spans.
 */
public class SpanTagger {

    /**
     * Prevents Instantiation.
     */
    private SpanTagger() {}

    /**
     * Sets tags to the span which is contained in the provided span wrapper, from information acquired from the
     * given basic statistic data unit.
     * @param spanWrapper               Span wrapper that contains the target span.
     */
    public static void setSpanTags(SpanWrapper spanWrapper) {
        StatisticsLog openStatisticsLog = new StatisticsLog(spanWrapper.getStatisticDataUnit());
        Span span = spanWrapper.getSpan();
        if (OpenTelemetryManagerHolder.isCollectingPayloads() || OpenTelemetryManagerHolder.isCollectingProperties()) {
            if (OpenTelemetryManagerHolder.isCollectingPayloads()) {
                if(openStatisticsLog.getBeforePayload() != null) {
                    span.setAttribute("beforePayload", openStatisticsLog.getBeforePayload());
                }
                StatisticDataUnit statisticDataUnit = spanWrapper.getCloseEventStatisticDataUnit();
                if (statisticDataUnit != null) {
                    if (statisticDataUnit.getPayload() != null) {
                        span.setAttribute("afterPayload", statisticDataUnit.getPayload());
                    }
                } else if (openStatisticsLog.getBeforePayload() !=null){
                    //This means a close event hasn't been triggered so payload is equal to before payload
                    span.setAttribute("afterPayload", openStatisticsLog.getBeforePayload());
                }
            }

            if (OpenTelemetryManagerHolder.isCollectingProperties()) {
                if (spanWrapper.getStatisticDataUnit().getContextPropertyMap() != null) {
                    span.setAttribute("beforeContextPropertyMap",
                            spanWrapper.getStatisticDataUnit().getContextPropertyMap().toString());
                }
                if (spanWrapper.getCloseEventStatisticDataUnit() != null) {
                    if (spanWrapper.getCloseEventStatisticDataUnit().getContextPropertyMap() != null) {
                        span.setAttribute("afterContextPropertyMap",
                                spanWrapper.getCloseEventStatisticDataUnit().getContextPropertyMap().toString());
                    }
                } else if (openStatisticsLog.getContextPropertyMap() != null) {
                    span.setAttribute("afterContextPropertyMap", openStatisticsLog.getContextPropertyMap().toString());
                }
                if (spanWrapper.getCloseEventStatisticDataUnit() != null &&
                        spanWrapper.getCloseEventStatisticDataUnit().getPropertyValue() != null) {
                    span.setAttribute("propertyMediatorValue",
                            spanWrapper.getCloseEventStatisticDataUnit().getPropertyValue());
                }
            }
        }
        if (openStatisticsLog.getComponentName() != null) {
            span.setAttribute("componentName", openStatisticsLog.getComponentName());
        }
        if(openStatisticsLog.getComponentTypeToString() != null){
            span.setAttribute("componentType", openStatisticsLog.getComponentTypeToString());
        }
        span.setAttribute("threadId", Thread.currentThread().getId());
        if(openStatisticsLog.getComponentId() != null){
            span.setAttribute("componentId", openStatisticsLog.getComponentId());
        }
        if(openStatisticsLog.getHashCode() != null){
            span.setAttribute("hashcode", openStatisticsLog.getHashCode());
        }
        if (openStatisticsLog.getTransportHeaderMap() != null) {
            span.setAttribute("Transport Headers", openStatisticsLog.getTransportHeaderMap().toString());
        }

        if (openStatisticsLog.getStatusCode() != null) {
            span.setAttribute("Status code", openStatisticsLog.getStatusCode());
        }
        if (openStatisticsLog.getStatusDescription() != null) {
            span.setAttribute("Status description", openStatisticsLog.getStatusDescription());
        }
        if (openStatisticsLog.getEndpoint() != null) {
            span.setAttribute("Endpoint", String.valueOf(openStatisticsLog.getEndpoint().getJsonRepresentation()));
        }
    }
}
