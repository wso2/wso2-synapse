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
import org.apache.synapse.config.xml.endpoints.EndpointFactory;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.builtin.CallMediator;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.Properties;

/**
 * Factory for {@link org.apache.synapse.mediators.builtin.CallMediator} instances.
 * <p>
 * The &lt;call&gt; element is used to send messages out of Synapse to some endpoint. In the simplest case,
 * the place to send the message to is implicit in the message (via a property of the message itself)-
 * that is indicated by the following:
 * <pre>
 *  &lt;call/&gt;
 * </pre>
 *
 * If the message is to be sent to a endpoint, then the following is used:
 * <pre>
 *  &lt;call&gt;
 *   (endpointref | endpoint)
 *  &lt;/call&gt;
 * </pre>
 * where the endpointref token refers to the following:
 * <pre>
 * &lt;endpoint ref="name"/&gt;
 * </pre>
 * and the endpoint token refers to an anonymous endpoint defined inline:
 * <pre>
 *  &lt;endpoint address="url"/&gt;
 * </pre>
 * If the message is to be sent to an endpoint selected by load balancing across a set of endpoints,
 * then it is indicated by the following:
 * <pre>
 * &lt;call&gt;
 *   &lt;load-balance algorithm="uri"&gt;
 *     (endpointref | endpoint)+
 *   &lt;/load-balance&gt;
 * &lt;/call&gt;
 * </pre>
 * Similarly, if the message is to be sent to an endpoint with failover semantics, then it is indicated by the following:
 * <pre>
 * &lt;call&gt;
 *   &lt;failover&gt;
 *     (endpointref | endpoint)+
 *   &lt;/failover&gt;
 * &lt;/call&gt;
 * </pre>
 */
public class CallMediatorFactory extends AbstractMediatorFactory {

    private static final QName CALL_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "call");
    private static final QName ENDPOINT_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "endpoint");
    private static final QName BLOCKING_Q = new QName("blocking");
    private static final QName ATT_INIT_AXIS2_CLIENT_OPTIONS = new QName("initAxis2ClientOptions");
    private static final QName ATT_AXIS2XML = new QName("axis2xml");
    private static final QName ATT_REPOSITORY = new QName("repository");

    public Mediator createSpecificMediator(OMElement elem, Properties properties) {

        CallMediator callMediator = new CallMediator();

        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        processAuditStatus(callMediator, elem);

        OMElement epElement = elem.getFirstChildWithName(ENDPOINT_Q);
        if (epElement != null) {
            // create the endpoint and set it in the call mediator
            Endpoint endpoint = EndpointFactory.getEndpointFromElement(epElement, true, properties);
            if (endpoint != null) {
                callMediator.setEndpoint(endpoint);
            }
        }

        OMAttribute blockingAtt = elem.getAttribute(BLOCKING_Q);
        if (blockingAtt != null) {
            callMediator.setBlocking(Boolean.parseBoolean(blockingAtt.getAttributeValue()));
            if (callMediator.isBlocking()) {
                OMAttribute initAxis2ClientOptions = elem.getAttribute(ATT_INIT_AXIS2_CLIENT_OPTIONS);
                OMAttribute axis2xmlAttr = elem.getAttribute(ATT_AXIS2XML);
                OMAttribute repoAttr = elem.getAttribute(ATT_REPOSITORY);

                if (initAxis2ClientOptions != null) {
                    callMediator.setInitClientOptions(Boolean.parseBoolean(initAxis2ClientOptions.getAttributeValue()));
                }

                if (axis2xmlAttr != null && axis2xmlAttr.getAttributeValue() != null) {
                    File axis2xml = new File(axis2xmlAttr.getAttributeValue());
                    if (axis2xml.exists() && axis2xml.isFile()) {
                        callMediator.setAxis2xml(axis2xmlAttr.getAttributeValue());
                    } else {
                        handleException("Invalid axis2.xml path: " + axis2xmlAttr.getAttributeValue());
                    }
                }

                if (repoAttr != null && repoAttr.getAttributeValue() != null) {
                    File repo = new File(repoAttr.getAttributeValue());
                    if (repo.exists() && repo.isDirectory()) {
                        callMediator.setClientRepository(repoAttr.getAttributeValue());
                    } else {
                        handleException("Invalid repository path: " + repoAttr.getAttributeValue());
                    }
                }
            }
        }
        addAllCommentChildrenToList(elem, callMediator.getCommentsList());

        return callMediator;

    }

    public QName getTagQName() {
        return CALL_Q;
    }
}
