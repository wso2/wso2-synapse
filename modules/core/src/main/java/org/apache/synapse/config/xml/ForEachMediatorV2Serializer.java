/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
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
import org.apache.synapse.mediators.v2.ForEachMediatorV2;

/**
 * Serializer for ForEach V2 mediator.
 */
public class ForEachMediatorV2Serializer extends AbstractMediatorSerializer {

    public String getMediatorClassName() {

        return ForEachMediatorV2.class.getName();
    }

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {

        if (m instanceof ForEachMediatorV2) {
            OMElement forEachElem = fac.createOMElement("foreach", synNS);
            saveTracingState(forEachElem, m);

            ForEachMediatorV2 forEachMediatorV2 = (ForEachMediatorV2) m;

            if (forEachMediatorV2.getCollectionExpression() != null) {
                SynapsePathSerializer.serializePath(forEachMediatorV2.getCollectionExpression(),
                        forEachMediatorV2.getCollectionExpression().getExpression(), forEachElem, "collection");
            } else {
                handleException("Missing collection of the ForEach which is required.");
            }
            forEachElem.addAttribute(fac.createOMAttribute(
                    "parallel-execution", nullNS, Boolean.toString(forEachMediatorV2.getParallelExecution())));
            if (forEachMediatorV2.isContinueWithoutAggregation()) {
                forEachElem.addAttribute(fac.createOMAttribute(
                        ForEachMediatorFactory.ATT_CONTINUE_WITHOUT_AGGREGATION.getLocalPart(), nullNS, "true"));
            } else {
                forEachElem.addAttribute(fac.createOMAttribute(
                        ForEachMediatorFactory.ATT_UPDATE_ORIGINAL.getLocalPart(), nullNS, Boolean.toString(forEachMediatorV2.isUpdateOriginal())));
                if (!forEachMediatorV2.isUpdateOriginal()) {
                    forEachElem.addAttribute(fac.createOMAttribute(
                            AbstractMediatorFactory.ATT_TARGET_VARIABLE.getLocalPart(), nullNS, forEachMediatorV2.getVariableName()));
                    forEachElem.addAttribute(fac.createOMAttribute(
                            AbstractMediatorFactory.RESULT_TYPE_Q.getLocalPart(), nullNS, forEachMediatorV2.getContentType()));
                    if ("XML".equalsIgnoreCase(forEachMediatorV2.getContentType())) {
                        forEachElem.addAttribute(fac.createOMAttribute(
                                AbstractMediatorFactory.ATT_ROOT_ELEMENT.getLocalPart(), nullNS, forEachMediatorV2.getRootElementName()));
                    }
                }
            }
            if (forEachMediatorV2.getCounterVariable() != null) {
                forEachElem.addAttribute(fac.createOMAttribute(
                        "counter-variable", nullNS, forEachMediatorV2.getCounterVariable()));
            }
            if (forEachMediatorV2.getTarget() != null) {
                if (forEachMediatorV2.getTarget() != null && forEachMediatorV2.getTarget().getSequence() != null) {
                    SequenceMediatorSerializer serializer = new SequenceMediatorSerializer();
                    serializer.serializeAnonymousSequence(forEachElem, forEachMediatorV2.getTarget().getSequence());
                }
            } else {
                handleException("Missing sequence element of the ForEach which is required.");
            }
            serializeComments(forEachElem, forEachMediatorV2.getCommentsList());
            return forEachElem;
        } else {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
            return null;
        }
    }
}
