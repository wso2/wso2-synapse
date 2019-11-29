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

package org.apache.synapse.aspects.flow.statistics.opentracing.management.helpers;

import io.opentracing.Span;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;
import org.apache.synapse.aspects.flow.statistics.opentracing.OpenTracingManagerHolder;
import org.apache.synapse.aspects.flow.statistics.opentracing.models.SpanWrapper;

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
     * @param basicStatisticDataUnit    Basic statistic data unit from which, tag data will be acquired.
     */
    public static void setSpanTags(SpanWrapper spanWrapper, BasicStatisticDataUnit basicStatisticDataUnit) {
        StatisticsLog statisticsLog = new StatisticsLog(spanWrapper.getStatisticDataUnit());
        Span span = spanWrapper.getSpan();
        if (basicStatisticDataUnit instanceof StatisticDataUnit) {
            if (OpenTracingManagerHolder.isCollectingPayloads() || OpenTracingManagerHolder.isCollectingProperties()) {
                StatisticDataUnit endEventDataUnit = (StatisticDataUnit) basicStatisticDataUnit;

                if (OpenTracingManagerHolder.isCollectingPayloads()) {
                    statisticsLog.setAfterPayload(endEventDataUnit.getPayload());
                    span.setTag("beforePayload", statisticsLog.getBeforePayload());
                    span.setTag("afterPayload", statisticsLog.getAfterPayload());
                }

                if (OpenTracingManagerHolder.isCollectingProperties()) {
                    if (spanWrapper.getStatisticDataUnit().getContextPropertyMap() != null) {
                        span.setTag("beforeContextPropertyMap",
                                spanWrapper.getStatisticDataUnit().getContextPropertyMap().toString());
                    }
                    if (statisticsLog.getContextPropertyMap() != null) {
                        span.setTag("afterContextPropertyMap", statisticsLog.getContextPropertyMap().toString());
                    }
                }
            }

            span.setTag("componentName", statisticsLog.getComponentName());
            span.setTag("componentType", statisticsLog.getComponentTypeToString());
            span.setTag("threadId", Thread.currentThread().getId());
            span.setTag("componentId", statisticsLog.getComponentId());
            span.setTag("hashcode", statisticsLog.getHashCode());
        }
    }
}
