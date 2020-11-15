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
import org.apache.axiom.om.OMText;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.endpoints.EndpointSerializer;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.builtin.CallMediator;
import org.apache.synapse.mediators.elementary.EnrichMediator;
import org.apache.synapse.mediators.elementary.Source;
import org.apache.synapse.mediators.elementary.Target;

/**
 * Serializer for {@link org.apache.synapse.mediators.builtin.CallMediator} instances.
 *
 * @see org.apache.synapse.config.xml.CallMediatorFactory
 */
public class CallMediatorSerializer extends AbstractMediatorSerializer {

    public OMElement serializeSpecificMediator(Mediator m) {

        if (!(m instanceof CallMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        CallMediator mediator = (CallMediator) m;
        OMElement call = fac.createOMElement("call", synNS);
        saveTracingState(call, mediator);

        Endpoint activeEndpoint = mediator.getEndpoint();
        if (activeEndpoint != null) {
            call.addChild(EndpointSerializer.getElementFromEndpoint(activeEndpoint));
        }
        if (mediator.isBlocking()) {
            call.addAttribute(fac.createOMAttribute("blocking", nullNS, "true"));
            if (!mediator.getInitClientOptions()) {
                call.addAttribute(fac.createOMAttribute("initAxis2ClientOptions", nullNS, "false"));
            }
            if (mediator.getClientRepository() != null) {
                call.addAttribute(fac.createOMAttribute("repository", nullNS, mediator.getClientRepository()));
            }
            if (mediator.getAxis2xml() != null) {
                call.addAttribute(fac.createOMAttribute("axis2xml", nullNS, mediator.getAxis2xml()));
            }
        }
        if (mediator.isSourceAvailable()) {
            OMElement sourceEle = serializeSource(mediator);
            call.addChild(sourceEle);
        }

        if (mediator.isTargetAvailable()) {
            OMElement targetEle = serializeTarget(mediator.getTargetForInboundPayload());
            call.addChild(targetEle);
        }

        serializeComments(call, mediator.getCommentsList());

        return call;
    }

    private OMElement serializeSource(CallMediator mediator) {
        Source source = mediator.getSourceForOutboundPayload();
        String sourceContentType = mediator.getSourceMessageType();

        OMElement sourceEle = fac.createOMElement("source", synNS);
        sourceEle.addAttribute(fac.createOMAttribute("type", nullNS,
                intTypeToString(source.getSourceType())));

        if (sourceContentType != null) {
            sourceEle.addAttribute(fac.createOMAttribute("contentType", nullNS, sourceContentType));
        }

        if (source.getSourceType() == EnrichMediator.PROPERTY) {
            sourceEle.setText(source.getProperty());
        } else if (source.getSourceType() == EnrichMediator.CUSTOM) {
            sourceEle.setText(source.getXpath().toString());
        } else if (source.getSourceType() == EnrichMediator.INLINE) {
            if (source.getInlineOMNode() instanceof OMElement) {
                sourceEle.addChild(((OMElement) source.getInlineOMNode()).cloneOMElement());
            } else if (source.getInlineOMNode() instanceof OMText) {
                sourceEle.setText(((OMText) source.getInlineOMNode()).getText());
            }
        }
        return sourceEle;
    }

    private OMElement serializeTarget(Target target) {
        OMElement targetEle = fac.createOMElement("target", synNS);
        targetEle.addAttribute(fac.createOMAttribute("type", nullNS,
                intTypeToString(target.getTargetType())));
        if (target.getTargetType() == EnrichMediator.PROPERTY) {
            targetEle.setText(target.getProperty());
        }

        return targetEle;
    }

    private String intTypeToString(int type) {
        if (type == EnrichMediator.CUSTOM) {
            return EnrichMediatorFactory.CUSTOM;
        } else if (type == EnrichMediator.BODY) {
            return EnrichMediatorFactory.BODY;
        } else if (type == EnrichMediator.PROPERTY) {
            return EnrichMediatorFactory.PROPERTY;
        } else if (type == EnrichMediator.INLINE) {
            return EnrichMediatorFactory.INLINE;
        }
        return null;
    }

    public String getMediatorClassName() {
        return CallMediator.class.getName();
    }
}
