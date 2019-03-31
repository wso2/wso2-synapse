/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.store.MessageStoreMediator;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.Properties;

/**
 * Creates an instance of a MessageStore mediator using XML configuration specified
 *
 * <pre>
 * &lt;store messageStore = "message store name" [sequence = "sequence name"] /&gt;
 * </pre>
 *
 * TODO Message store mediator will be improved with more user options
 */
public class MessageStoreMediatorFactory extends AbstractMediatorFactory{

    private static final QName STORE_Q    = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "store");
    private static final QName ATT_MESSAGE_STORE   = new QName("messageStore");
    private static final QName ATT_SEQUENCE   = new QName("sequence");

    @Override
    protected Mediator createSpecificMediator(OMElement elem, Properties properties) {
        MessageStoreMediator messageStoreMediator = new MessageStoreMediator();
        processAuditStatus(messageStoreMediator, elem);
        OMAttribute nameAtt = elem.getAttribute(ATT_NAME);
        if(nameAtt != null) {
            messageStoreMediator.setName(nameAtt.getAttributeValue());
        }

        OMAttribute messageStoreNameAtt = elem.getAttribute(ATT_MESSAGE_STORE);

        if (messageStoreNameAtt != null) {
            if (checkForExpression(messageStoreNameAtt)) {
                String path = messageStoreNameAtt.getAttributeValue().substring(1, messageStoreNameAtt.getAttributeValue().length()-1);
                try {
                    messageStoreMediator.setMessageStoreExp(SynapsePathFactory.getSynapsePath(elem, path));
                } catch (JaxenException e) {
                    String msg = "Invalid XPath expression for attribute 'messageStore' : " + messageStoreNameAtt
                            .getAttributeValue();
                    throw new SynapseException(msg, e);
                }
            } else {
                messageStoreMediator.setMessageStoreName(messageStoreNameAtt.getAttributeValue());
            }
        } else {
            throw new SynapseException("Message Store mediator must have a Message store defined");
        }

        OMAttribute sequenceAtt = elem.getAttribute(ATT_SEQUENCE);

        if(sequenceAtt != null) {
            messageStoreMediator.setOnStoreSequence(sequenceAtt.getAttributeValue());
        }

        addAllCommentChildrenToList(elem, messageStoreMediator.getCommentsList());

        return messageStoreMediator;
    }

    public QName getTagQName() {
       return STORE_Q;
    }

    /**
     * Checks whether the value of the attribute is enclosed by curly braces.
     * If yes, the value is taken as an expression.
     * Else, it will be a string value.
     *
     * @param atr the atrribute whose value is checked for enclosing curly braces.
     * @return true if the value is enclosed by curly braces.
     */
    private boolean checkForExpression(OMAttribute atr){
        return ((atr.getAttributeValue().startsWith("{")) && (atr.getAttributeValue().endsWith("}")));
    }
}
