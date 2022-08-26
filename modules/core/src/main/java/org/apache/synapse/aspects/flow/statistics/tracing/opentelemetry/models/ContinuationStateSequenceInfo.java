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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models;

import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.helpers.TracingUtils;

/**
 * Contains information about a sequence's continuation state.
 * This is used to hold open events to start a span - until an appropriate continuation state stack insertion event
 * is received,
 * as well as finishing the span when the relevant sequence is removed from the continuation state stack.
 */
public class ContinuationStateSequenceInfo {
    private StatisticDataUnit statisticDataUnit;
    private String spanReferenceId;
    private boolean isSpanActive;

    public ContinuationStateSequenceInfo(StatisticDataUnit statisticDataUnit) {
        this.statisticDataUnit = statisticDataUnit;
        this.spanReferenceId = TracingUtils.extractId(statisticDataUnit);
        this.isSpanActive = false;
    }

    public StatisticDataUnit getStatisticDataUnit() {
        return statisticDataUnit;
    }

    public String getSpanReferenceId() {
        return spanReferenceId;
    }

    public boolean isSpanActive() {
        return isSpanActive;
    }

    public void setSpanActive(boolean spanActive) {
        isSpanActive = spanActive;
    }
}
