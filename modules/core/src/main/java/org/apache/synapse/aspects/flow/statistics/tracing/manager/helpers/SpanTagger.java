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

package org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers;

import io.opentracing.Span;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanWrapper;

public class SpanTagger {
    public static void setDebugSpanTags(Span span, String absoluteIndex, StatisticDataUnit statisticDataUnit,
                                        MessageContext synCtx) {
        if (statisticDataUnit != null) {
            span.setTag("debug.allIds",
                    statisticDataUnit.getCurrentIndex() + "(" + absoluteIndex + ")[" +statisticDataUnit.getParentIndex() + "]");
            span.setTag("debug.absoluteIndex", absoluteIndex);

            span.setTag("debug.synCtx", synCtx.toString());
            span.setTag("debug.synCtxReference", Util.getObjectReference(synCtx));

            // BasicStatisticUnit level (parent)
            span.setTag("debug.currentIndex", statisticDataUnit.getCurrentIndex());
            span.setTag("debug.statisticId", statisticDataUnit.getStatisticId());
            span.setTag("debug.isTracingEnabled", statisticDataUnit.isTracingEnabled());
            span.setTag("debug.isOutOnlyFlow", statisticDataUnit.isOutOnlyFlow());

            // StatisticDataUnit level
            span.setTag("debug.parentIndex", statisticDataUnit.getParentIndex());
            span.setTag("debug.shouldTrackParent", statisticDataUnit.isShouldTrackParent());
            span.setTag("debug.continuationCall", statisticDataUnit.isContinuationCall());
            span.setTag("debug.flowContinuableMediator", statisticDataUnit.isFlowContinuableMediator());
            span.setTag("debug.flowSplittingMediator", statisticDataUnit.isFlowSplittingMediator());
            span.setTag("debug.flowAggregateMediator", statisticDataUnit.isFlowAggregateMediator());
            span.setTag("debug.isIndividualStatisticCollected", statisticDataUnit.isIndividualStatisticCollected());

            if (statisticDataUnit.getParentList() != null) {
                StringBuilder parentList = new StringBuilder("[");
                for (Integer integer : statisticDataUnit.getParentList()) {
                    parentList.append(integer).append(", ");
                }
                parentList.append("]");
                span.setTag("debug.parentList", parentList.toString());
            } else {
                span.setTag("debug.parentList", "null");
            }
        }
        span.setTag("debug.threadId", Thread.currentThread().getId());
    }

    public static void setSpanTags(SpanWrapper spanWrapper, BasicStatisticDataUnit basicStatisticDataUnit) {
        StatisticsLog statisticsLog = new StatisticsLog(spanWrapper.getStatisticDataUnit());
        Span span = spanWrapper.getSpan();
        if (basicStatisticDataUnit instanceof StatisticDataUnit) {
            StatisticDataUnit endEventDataUnit = (StatisticDataUnit) basicStatisticDataUnit;
            statisticsLog.setAfterPayload(endEventDataUnit.getPayload());
            span.setTag("noOfFaults", statisticsLog.getNoOfFaults());
            span.setTag("componentName", statisticsLog.getComponentName());
            span.setTag("componentType", statisticsLog.getComponentTypeToString());
            span.setTag("componentId", statisticsLog.getComponentId());
            span.setTag("hashcode", statisticsLog.getHashCode());
            span.setTag("beforePayload", statisticsLog.getBeforePayload());
            span.setTag("afterPayload", statisticsLog.getAfterPayload());
        }
    }
}
