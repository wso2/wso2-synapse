/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.ForEachMediator;
import org.apache.synapse.mediators.v2.ForEachMediatorV2;

/**
 * <p>Serialize for each mediator as below : </p>
 * <p/>
 * <pre>
 * &lt;foreach expression="xpath|jsonpath" [sequence="sequence_ref"] [id="foreach_id"] &gt;
 *     &lt;sequence&gt;
 *       (mediator)+
 *     &lt;/sequence&gt;?
 * &lt;/foreach&gt;
 * </pre>
 */
public class ForEachMediatorSerializer extends AbstractMediatorSerializer {

    public String getMediatorClassName() {
        return ForEachMediator.class.getName();
    }

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {
        if (m instanceof ForEachMediator) {
            OMElement forEachElem = fac.createOMElement("foreach", synNS);
            saveTracingState(forEachElem, m);

            ForEachMediator forEachMed = (ForEachMediator) m;

            if (forEachMed.getId() != null) {
                forEachElem.addAttribute("id", forEachMed.getId(), nullNS);
            }

            if (forEachMed.getExpression() != null) {
                SynapsePathSerializer.serializePath(forEachMed.getExpression(),
                        forEachElem, "expression");
            } else {
                handleException("Missing expression of the ForEach which is required.");
            }

            if (forEachMed.getSequenceRef() != null) {
                forEachElem.addAttribute("sequence", forEachMed.getSequenceRef(), null);
            } else if (forEachMed.getSequence() != null) {
                SequenceMediatorSerializer seqSerializer = new SequenceMediatorSerializer();
                OMElement seqElement = seqSerializer.serializeAnonymousSequence(
                        null, forEachMed.getSequence());
                seqElement.setLocalName("sequence");
                forEachElem.addChild(seqElement);
            }

            serializeComments(forEachElem, forEachMed.getCommentsList());

            return forEachElem;
        } else if (m instanceof ForEachMediatorV2) {
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
                        "continue-without-aggregation", nullNS, "true"));
            } else {
                forEachElem.addAttribute(fac.createOMAttribute(
                        "update-original", nullNS, Boolean.toString(forEachMediatorV2.isUpdateOriginal())));
                if (!forEachMediatorV2.isUpdateOriginal()) {
                    forEachElem.addAttribute(fac.createOMAttribute(
                            "target", nullNS, "variable"));
                    forEachElem.addAttribute(fac.createOMAttribute(
                            "variable-name", nullNS, forEachMediatorV2.getVariableName()));
                    forEachElem.addAttribute(fac.createOMAttribute(
                            "result-type", nullNS, forEachMediatorV2.getContentType()));
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
