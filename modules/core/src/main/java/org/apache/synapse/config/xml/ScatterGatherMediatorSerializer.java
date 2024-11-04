/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.mediators.v2.ScatterGather;

/**
 * Serializer for {@link ScatterGather} instances.
 */
public class ScatterGatherMediatorSerializer extends AbstractMediatorSerializer {

    public OMElement serializeSpecificMediator(Mediator m) {

        ScatterGather scatterGatherMediator = null;
        if (!(m instanceof ScatterGather)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        } else {
            scatterGatherMediator = (ScatterGather) m;
        }

        assert scatterGatherMediator != null;
        OMElement scatterGatherElement = fac.createOMElement("scatter-gather", synNS);
        saveTracingState(scatterGatherElement, scatterGatherMediator);

        scatterGatherElement.addAttribute(fac.createOMAttribute(
                "parallel-execution", nullNS, Boolean.toString(scatterGatherMediator.getParallelExecution())));
        OMElement aggregationElement = fac.createOMElement("aggregation", synNS);

        SynapsePathSerializer.serializePath(
                scatterGatherMediator.getAggregationExpression(), aggregationElement, "value-to-aggregate");

        if (scatterGatherMediator.getCorrelateExpression() != null) {
            SynapsePathSerializer.serializePath(
                    scatterGatherMediator.getAggregationExpression(), aggregationElement, "condition");
        }

        if (scatterGatherMediator.getCompletionTimeoutMillis() != 0) {
            aggregationElement.addAttribute(fac.createOMAttribute(
                    "timeout", nullNS, Long.toString(scatterGatherMediator.getCompletionTimeoutMillis())));
        }
        if (scatterGatherMediator.getMinMessagesToComplete() != null) {
            new ValueSerializer().serializeValue(
                    scatterGatherMediator.getMinMessagesToComplete(), "min-messages", aggregationElement);
        }
        if (scatterGatherMediator.getMaxMessagesToComplete() != null) {
            new ValueSerializer().serializeValue(
                    scatterGatherMediator.getMaxMessagesToComplete(), "max-messages", aggregationElement);
        }
        scatterGatherElement.addChild(aggregationElement);

        for (Target target : scatterGatherMediator.getTargets()) {
            if (target != null) {
                scatterGatherElement.addChild(TargetSerializer.serializeTarget(target));
            }
        }
        serializeComments(scatterGatherElement, scatterGatherMediator.getCommentsList());

        return scatterGatherElement;
    }

    public String getMediatorClassName() {

        return ScatterGather.class.getName();
    }
}
