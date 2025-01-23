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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.CallMediator;
import org.apache.synapse.mediators.builtin.CalloutMediator;
import org.apache.synapse.mediators.builtin.ForEachMediator;
import org.apache.synapse.mediators.builtin.SendMediator;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.mediators.v2.ForEachMediatorV2;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Properties;

/**
 * <p></p>The &lt;foreach&gt; mediator is used to split to messages by the given XPath/JSONPath expression
 * and processed as per the provided sequence.
 * <p/>
 * <p/>
 * <pre>
 * &lt;foreach expression="xpath|jsonpath" [sequence="sequence_ref"] [id="foreach_id"] &gt;
 *     &lt;sequence&gt;
 *       (mediator)+
 *     &lt;/sequence&gt;?
 * &lt;/foreach&gt;
 * </pre>
 */
public class ForEachMediatorFactory extends AbstractMediatorFactory {

    private static final QName FOREACH_Q =
            new QName(SynapseConstants.SYNAPSE_NAMESPACE, "foreach");

    private static final QName ID_Q
            = new QName(XMLConfigConstants.NULL_NAMESPACE, "id");

    private static final QName CONTINUE_IN_FAULT_Q
            = new QName(XMLConfigConstants.NULL_NAMESPACE, "continueLoopOnFailure");

    private static final QName ATT_COLLECTION = new QName("collection");
    private static final QName SEQUENCE_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "sequence");
    private static final QName PARALLEL_EXEC_Q = new QName("parallel-execution");
    private static final QName ATT_UPDATE_ORIGINAL = new QName("update-original");
    private static final QName ATT_TARGET_VARIABLE_NAME = new QName("target-variable");
    private static final QName ATT_COUNTER_VARIABLE = new QName("counter-variable");
    private static final QName ATT_CONTINUE_WITHOUT_AGGREGATION  = new QName("continue-without-aggregation");

    public QName getTagQName() {
        return FOREACH_Q;
    }

    @Override
    protected Mediator createSpecificMediator(OMElement elem,
                                              Properties properties) {

        OMAttribute collectionAttr = elem.getAttribute(ATT_COLLECTION);
        if (collectionAttr != null && StringUtils.isNotBlank(collectionAttr.getAttributeValue())) {
            return createForEachMediatorV2(elem, properties);
        }

        ForEachMediator mediator = new ForEachMediator();
        processAuditStatus(mediator, elem);

        OMAttribute id = elem.getAttribute(ID_Q);
        if (id != null) {
            mediator.setId(id.getAttributeValue());
        }

        OMAttribute continueOnFail = elem.getAttribute(CONTINUE_IN_FAULT_Q);
        if (continueOnFail != null) {
            mediator.setContinueLoopOnFailure(Boolean.parseBoolean(continueOnFail.getAttributeValue()));
        }

        OMAttribute expression = elem.getAttribute(ATT_EXPRN);
        if (expression != null) {
            try {
                mediator.setExpression(SynapsePathFactory.getSynapsePath(elem, ATT_EXPRN));
            } catch (JaxenException e) {
                handleException("Unable to build the ForEach Mediator. " +
                                "Invalid XPath/JSONPath " +
                                expression.getAttributeValue(), e);
            }
        } else {
            handleException("XPath/JSONPath expression is required "
                            + "for an ForEach Mediator under the \"expression\" attribute");
        }

        OMAttribute sequenceAttr = elem.getAttribute(
                new QName(XMLConfigConstants.NULL_NAMESPACE, "sequence"));
        OMElement sequence;

        if (sequenceAttr != null && sequenceAttr.getAttributeValue() != null) {
            mediator.setSequenceRef(sequenceAttr.getAttributeValue());
        } else if ((sequence = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "sequence"))) != null) {
            SequenceMediatorFactory fac = new SequenceMediatorFactory();
            SequenceMediator sequenceMediator = fac.createAnonymousSequence(sequence, properties);
            if (validateSequence(sequenceMediator)) {
                mediator.setSequence(sequenceMediator);
            } else {
                handleException("Sequence cannot contain Call, Send or CallOut mediators");
            }
        }

        addAllCommentChildrenToList(elem, mediator.getCommentsList());

        return mediator;
    }

    private boolean validateSequence(SequenceMediator sequence) {
        if (sequence != null) {
            List<Mediator> mediators = sequence.getList();
            for (Mediator m : mediators) {
                if (m instanceof CallMediator || m instanceof CalloutMediator ||
                    m instanceof SendMediator) {
                    return false;
                }
            }
        }
        return true;
    }

    public Mediator createForEachMediatorV2(OMElement elem, Properties properties) {

        boolean asynchronousExe = true;

        ForEachMediatorV2 mediator = new ForEachMediatorV2();
        processAuditStatus(mediator, elem);

        OMAttribute parallelExecAttr = elem.getAttribute(PARALLEL_EXEC_Q);
        if (parallelExecAttr != null && parallelExecAttr.getAttributeValue().equals("false")) {
            asynchronousExe = false;
        }
        mediator.setParallelExecution(asynchronousExe);

        String continueWithoutAggregationAttr = elem.getAttributeValue(ATT_CONTINUE_WITHOUT_AGGREGATION);
        // If the continue-without-aggregation attribute is set to true, the mediator will not wait for the aggregation
        if ("true".equalsIgnoreCase(continueWithoutAggregationAttr)) {
            mediator.setContinueWithoutAggregation(true);
        } else {
            OMAttribute updateOriginalAttr = elem.getAttribute(ATT_UPDATE_ORIGINAL);
            if (updateOriginalAttr != null && "false".equals(updateOriginalAttr.getAttributeValue())) {
                mediator.setUpdateOriginal(false);
                String contentTypeAttr = elem.getAttributeValue(RESULT_TYPE_Q);
                String variableNameAttr = elem.getAttributeValue(ATT_TARGET_VARIABLE_NAME);
                if (StringUtils.isNotBlank(contentTypeAttr) && StringUtils.isNotBlank(variableNameAttr)) {
                    mediator.setVariableName(variableNameAttr);
                    if ("JSON".equals(contentTypeAttr)) {
                        mediator.setContentType(ForEachMediatorV2.JSON_TYPE);
                    } else if ("XML".equals(contentTypeAttr)) {
                        String rootElementAttr = elem.getAttributeValue(ATT_ROOT_ELEMENT);
                        if (StringUtils.isNotBlank(rootElementAttr)) {
                            mediator.setRootElementName(rootElementAttr);
                            mediator.setContentType(ForEachMediatorV2.XML_TYPE);
                        } else {
                            String msg = "The 'result-enclosing-element' attribute is required for the configuration of a " +
                                    "Foreach mediator when the 'result-type' is 'XML'";
                            throw new SynapseException(msg);
                        }
                    } else {
                        handleException("The 'result-content-type' attribute should be either 'JSON' or 'XML'");
                    }
                } else {
                    handleException("The 'result-content-type' and 'target-variable' attributes are required when the " +
                            "'update-original' attribute is 'false'");
                }
            }
        }

        OMAttribute counterVariableAttr = elem.getAttribute(ATT_COUNTER_VARIABLE);
        if (counterVariableAttr != null && StringUtils.isNotBlank(counterVariableAttr.getAttributeValue())) {
            if (asynchronousExe) {
                handleException("The 'counter-variable' attribute is not allowed when parallel-execution is true");
            }
            mediator.setCounterVariable(counterVariableAttr.getAttributeValue());
        }

        OMAttribute collectionAttr = elem.getAttribute(ATT_COLLECTION);
        if (collectionAttr == null || StringUtils.isBlank(collectionAttr.getAttributeValue())) {
            handleException("The 'collection' attribute is required for the configuration of a Foreach mediator");
        } else {
            try {
                mediator.setCollectionExpression(SynapsePathFactory.getSynapsePath(elem, ATT_COLLECTION));
            } catch (JaxenException e) {
                handleException("Unable to build the Foreach Mediator. Invalid expression "
                        + collectionAttr.getAttributeValue(), e);
            }
        }

        OMElement sequenceElement = elem.getFirstChildWithName(SEQUENCE_Q);
        if (sequenceElement == null) {
            handleException("A 'sequence' element is required for the configuration of a Foreach mediator");
        } else {
            Target target = new Target();
            SequenceMediatorFactory fac = new SequenceMediatorFactory();
            target.setSequence(fac.createAnonymousSequence(sequenceElement, properties));
            target.setAsynchronous(asynchronousExe);
            mediator.setTarget(target);
        }
        addAllCommentChildrenToList(elem, mediator.getCommentsList());
        return mediator;
    }
}
