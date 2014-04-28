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

package org.apache.synapse.inbound.jms;

import java.io.InputStream;
import java.util.Properties;

import javax.jms.Message;
import javax.jms.TextMessage;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.builder.SOAPBuilder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.format.DataSourceMessageBuilder;
import org.apache.axis2.format.ManagedDataSource;
import org.apache.axis2.format.ManagedDataSourceFactory;
import org.apache.axis2.transport.TransportUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.synapse.inbound.InboundEndpointUtils;
import org.apache.synapse.inbound.InjectHandler;
import org.apache.synapse.inbound.jms.JMSPollingConsumer;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.commons.vfs.FileObjectDataSource;
import org.apache.synapse.commons.vfs.VFSConstants; 
import org.apache.synapse.core.SynapseEnvironment;

public class JMSInjectHandler implements InjectHandler {

	private static final Log log = LogFactory.getLog(JMSInjectHandler.class);
	
	private String injectingSeq;
	private String onErrorSeq;
    private SynapseEnvironment synapseEnvironment;

    
	public JMSInjectHandler(String injectingSeq, String onErrorSeq, SynapseEnvironment synapseEnvironment, Properties jmsProperties){
		this.injectingSeq = injectingSeq;
		this.onErrorSeq = onErrorSeq;
		this.synapseEnvironment = synapseEnvironment;
	}	 
	
	public void invoke(Object object){
		
		Message msg = (Message)object;
        try {
            org.apache.synapse.MessageContext msgCtx = createMessageContext();
            String contentType = msg.getJMSType();
            if (log.isDebugEnabled()) {
                log.debug("Processed JMS Message of Content-type : " + contentType);
            }             
    		MessageContext axis2MsgCtx = ((org.apache.synapse.core.axis2.Axis2MessageContext)msgCtx).getAxis2MessageContext();  
    		// Determine the message builder to use
            Builder builder;
            if (contentType == null) {
                log.debug("No content type specified. Using SOAP builder.");
                builder = new SOAPBuilder();
            } else {
                int index = contentType.indexOf(';');
                String type = index > 0 ? contentType.substring(0, index) : contentType;
                builder = BuilderUtil.getBuilderFromSelector(type, axis2MsgCtx);
                if (builder == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("No message builder found for type '" + type +
                                "'. Falling back to SOAP.");
                    }
                    builder = new SOAPBuilder();
                }
            }
    
            // set the message payload to the message context
        	String message = ((TextMessage) msg).getText();
        	InputStream in = new AutoCloseInputStream(InboundEndpointUtils.toInputStream(message));            
        
            //Inject the message to the sequence.
            OMElement documentElement = builder.processDocument(in, contentType, axis2MsgCtx);

            msgCtx.setEnvelope(TransportUtils.createSOAPEnvelope(documentElement));                               
            if (injectingSeq == null || injectingSeq.equals("")) {
                log.error("Sequence name not specified. Sequence : " + injectingSeq);
            }
            SequenceMediator seq = (SequenceMediator) synapseEnvironment.getSynapseConfiguration().getSequence(injectingSeq);
            seq.setErrorHandler(onErrorSeq);
            if (seq != null) {
                if (log.isDebugEnabled()) {
                    log.debug("injecting message to sequence : " + injectingSeq);
                }
                synapseEnvironment.injectAsync(msgCtx, seq);
            } else {
                log.error("Sequence: " + injectingSeq + " not found");
            }     
        } catch (Exception e) {
            log.error("Error while processing the JMS Message");                
        }		
	}
    /**
     * Create the initial message context for the file
     * */
    private org.apache.synapse.MessageContext createMessageContext() {
        org.apache.synapse.MessageContext msgCtx = synapseEnvironment.createMessageContext();
        MessageContext axis2MsgCtx = ((org.apache.synapse.core.axis2.Axis2MessageContext)msgCtx).getAxis2MessageContext();
        axis2MsgCtx.setServerSide(true);
        axis2MsgCtx.setMessageID(UUIDGenerator.getUUID());
        // There is a discrepency in what I thought, Axis2 spawns a nes threads to
        // send a message is this is TRUE - and I want it to be the other way
        msgCtx.setProperty(MessageContext.CLIENT_API_NON_BLOCKING, true);
        return msgCtx;
    }
    
}

