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
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.CallMediator;
import org.apache.synapse.mediators.builtin.CalloutMediator;
import org.apache.synapse.mediators.builtin.ForEachMediator;
import org.apache.synapse.mediators.builtin.SendMediator;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Properties;

/**
 * <p></p>The &lt;foreach&gt; mediator is used to split to messages by the given XPath expression
 * and processed as per the provided sequence.
 * <p/>
 * <p/>
 * <pre>
 * &lt;foreach expression="xpath" [sequence="sequence_ref"] [id="foreach_id"] &gt;
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

    public QName getTagQName() {
        return FOREACH_Q;
    }

    @Override
    protected Mediator createSpecificMediator(OMElement elem,
                                              Properties properties) {
        ForEachMediator mediator = new ForEachMediator();
        processAuditStatus(mediator, elem);

        OMAttribute id = elem.getAttribute(ID_Q);
        if (id != null) {
            mediator.setId(id.getAttributeValue());
        }

        OMAttribute expression = elem.getAttribute(ATT_EXPRN);
        if (expression != null) {
            try {
                mediator.setExpression(SynapseXPathFactory.getSynapseXPath(elem,
                                                                           ATT_EXPRN));
            } catch (JaxenException e) {
                handleException("Unable to build the ForEach Mediator. " +
                                "Invalid XPath " +
                                expression.getAttributeValue(), e);
            }
        } else {
            handleException("XPath expression is required "
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

}
