/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.inbound;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.dom.soap12.SOAP12Factory;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.base.SequenceMediator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class InboundEndpointUtils {
    private static final Log log = LogFactory.getLog(InboundEndpointUtils.class.getName());
    private static Map<String, InboundResponseSender> registeredResponseHandlers = new HashMap<String, InboundResponseSender>();

    public static Properties paramsToProperties(Map<String, String> params) {
        Properties props = new Properties();
        props.putAll(params);
        return props;
    }

    public static InputStream toInputStream(String str) {
        return new ByteArrayInputStream(str.getBytes());
    }

    public static org.apache.axis2.context.MessageContext createAxis2MessageContext(SynapseEnvironment se) {
        return ((Axis2SynapseEnvironment) se).getAxis2ConfigurationContext().createMessageContext();
    }

    public static MessageContext createSynapseMessageContext(org.apache.axis2.context.MessageContext axis2mc,
                                                             SynapseEnvironment se) {
        SynapseConfiguration configuration = se.getSynapseConfiguration();
        return new Axis2MessageContext(axis2mc, configuration, se);
    }

    public static org.apache.axis2.context.MessageContext attachMessage(SOAPEnvelope envelope,
                                                                        org.apache.axis2.context.MessageContext axis2Ctx) {
        if (envelope == null) {
            log.error("Cannot attach null SOAP Envelope.");
            return null;
        }

        AxisConfiguration axisConfig = axis2Ctx.getConfigurationContext().getAxisConfiguration();
        if (axisConfig == null) {
            log.warn("Cannot create AxisConfiguration. AxisConfiguration is null.");
            return null;
        }

        try {
            axis2Ctx.setEnvelope(envelope);
            return axis2Ctx;
        } catch (Exception e) {
            log.error("Cannot attach SOAPEnvelope to Message Context. Error:" + e.getMessage(), e);
            return null;
        }
    }

    public static org.apache.axis2.context.MessageContext attachMessage(String jsonMessage, org.apache.axis2.context.
            MessageContext axis2Ctx) {
        if (jsonMessage == null) {
            log.error("Cannot attach null JSON string.");
            return null;
        }

        AxisConfiguration axisConfig = axis2Ctx.getConfigurationContext().getAxisConfiguration();
        if (axisConfig == null) {
            log.warn("Cannot create AxisConfiguration. AxisConfiguration is null.");
            return null;
        }

        try {
            SOAPFactory soapFactory = new SOAP12Factory();
            SOAPEnvelope envelope = soapFactory.createSOAPEnvelope();
            envelope.addChild(JsonUtil.newJsonPayload(axis2Ctx, jsonMessage, true, true));
            axis2Ctx.setEnvelope(envelope);
            return axis2Ctx;
        } catch (Exception e) {
            log.error("Cannot attach message to Message Context. Error: " + e.getMessage(), e);
            return null;
        }
    }

    public static boolean injectMessage(MessageContext synCtx, SynapseEnvironment synapseEnvironment, String injectingSeq) {
        if (injectingSeq == null || injectingSeq.equals("")) {
            log.error("Sequence name not specified. Sequence : " + injectingSeq);
        }
        SequenceMediator seq = (SequenceMediator) synapseEnvironment.getSynapseConfiguration().getSequence(injectingSeq);
        if (seq != null) {
            if (log.isDebugEnabled()) {
                log.debug("Injecting message to sequence : " + injectingSeq);
            }
            synapseEnvironment.injectAsync(synCtx, seq);
            return true;
        } else {
            log.error("Sequence: " + injectingSeq + " not found");
            return false;
        }
    }

    public static void addResponseSender(String key, InboundResponseSender inboundResponseSender) {
        InboundResponseSender inboundResponseSenderexisit = getResponseSender(key);
        if (inboundResponseSenderexisit != null) {
            return;
        }
        registeredResponseHandlers.put(key, inboundResponseSender);
    }

    public static InboundResponseSender getResponseSender(String key) {
        return registeredResponseHandlers.get(key);
    }

}
