/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.builtin;

import java.io.InputStream;
import java.util.List;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import com.jayway.jsonpath.JsonPath;

public class ForEachMediator extends AbstractMediator {

	/** The XPath that will list the elements to be splitted */
	private SynapsePath expression = null;

	/** The target for the newly splitted messages */
	private Target target = null;

	public boolean mediate(MessageContext synCtx) {
		SynapseLog synLog = getLog(synCtx);

		if (synLog.isTraceOrDebugEnabled()) {
			synLog.traceOrDebug("FE*=Start : Foreach mediator");
			synLog.traceTrace("FE=Message : " + synCtx.getEnvelope());

			if (synLog.isTraceTraceEnabled()) {
				synLog.traceTrace("FE=Message : " + synCtx.getEnvelope());
			}
		}

		if (expression != null) {
			synLog.traceOrDebug("FE*=ForEach: expression = " +
			                    expression.toString());
		} else {
			synLog.traceOrDebug("FE*=ForEach: expression is null");
		}

		if (expression.getPathType().equals(SynapsePath.JSON_PATH)) {

			if (synLog.isTraceOrDebugEnabled()) {
				synLog.traceOrDebug("FE*=Splitting with Json : " + expression);

			}
			// org.apache.axis2.context.MessageContext axis2MessageContext =
			// ((Axis2MessageContext) synCtx).getAxis2MessageContext();

			// synLog.traceOrDebug("FE*=axis2MessageContext=" +
			// axis2MessageContext.toString());

			OMElement jsonElement = synCtx.getEnvelope().getBody().getFirstElement();

			synLog.traceOrDebug("FE*=Splitting Json : " + jsonElement);
			
			InputStream jsonPayLoad = JsonUtil.getJsonPayload( ((Axis2MessageContext) synCtx).getAxis2MessageContext());
			
			synLog.traceOrDebug("FE*=json payload : " + jsonPayLoad);
			
			String jsonString;
            try {
	            jsonString = JsonUtil.toJsonString(jsonElement).toString();
	            synLog.traceOrDebug("FE*=json string : " +jsonString );
	            String jsonExp = expression.getExpression();
	            if(jsonExp.startsWith("json-eval(")){
	            	jsonExp = jsonExp.substring(10, jsonExp.length()-1);
	            }
	            
	            List<?> splitElements = JsonPath.read(jsonString, jsonExp);
	            
	            int msgCount = splitElements.size();
				//int msgNumber = 0;

				if (synLog.isTraceOrDebugEnabled()) {
					synLog.traceOrDebug("FE*=Splitting with Json : " +
					                    expression + " resulted in " +
					                    msgCount + " elements");
				}
            } catch (AxisFault e1) {
	           
	            e1.printStackTrace();
            }
			
			
			
			
			// List<String> authors = JsonPath.read(json,
			// "$.store.book[*].author");

			// boolean test = JsonUtil.removeJsonPayload(axis2MessageContext);
			// synLog.traceOrDebug("FE*=test=" + test);

			return true;
		} else if (expression.getPathType().equals(SynapsePath.X_PATH)) {

			if (synLog.isTraceOrDebugEnabled()) {
				synLog.traceOrDebug("FE*=Splitting with Xpath : " + expression);

			}
			try {
				// get a copy of the message for the processing, if the
				// continueParent is set to true
				// this original message can go in further mediations and hence
				// we
				// should not change
				// the original message context FIXME: Needed?
				SOAPEnvelope envelope =
				                        MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope());
				// get the iteration elements and iterate through the list,
				// this call will also detach all the iteration elements
				List<?> splitElements =
				                        EIPUtils.getDetachedMatchingElements(envelope,
				                                                             synCtx,
				                                                             (SynapseXPath) expression);

				int msgCount = splitElements.size();
				int msgNumber = 0;

				if (synLog.isTraceOrDebugEnabled()) {
					synLog.traceOrDebug("FE*=Splitting with XPath : " +
					                    expression + " resulted in " +
					                    msgCount + " elements");
				}

				// iterate through the list
				for (Object o : splitElements) {
					if (!(o instanceof OMNode)) {
						handleException("Error splitting message with XPath : " +
						                        expression +
						                        " - result not an OMNode",
						                synCtx);
					}

					if (synLog.isTraceOrDebugEnabled()) {
						synLog.traceOrDebug("FE*=Submitting " +
						                    (msgNumber + 1) + " of " +
						                    msgNumber +
						                    " messages for processing in sequentially, in a general loop");
					}

					MessageContext iteratedMsgCtx =
					                                getIteratedMessage(synCtx,
					                                                   envelope,
					                                                   (OMNode) o);

					target.mediate(iteratedMsgCtx);
					EIPUtils.includeEnvelope(envelope,
					                         iteratedMsgCtx.getEnvelope(),
					                         synCtx, (SynapseXPath) expression);
					synCtx.setEnvelope(envelope);
				}

			} catch (JaxenException e) {
				handleException("Error evaluating split XPath expression : " +
				                expression, e, synCtx);
			} catch (AxisFault af) {
				handleException("Error creating an iterated copy of the message",
				                af, synCtx);
			}
			synLog.traceOrDebug("FE*=End : For Each mediator");
			return true;
		} else {
			handleException("Error evaluating  expression : " + expression,
			                synCtx);
			return false;
		}
	}

	/**
	 * Create a new message context using the given original message context,
	 * the envelope
	 * and the split result element.
	 * 
	 * @param synCtx
	 *            - original message context
	 * @param envelope
	 *            - envelope to be used in the iteration
	 * @param o
	 *            - element which participates in the iteration replacement
	 * @return newCtx created by the iteration
	 * @throws AxisFault
	 *             if there is a message creation failure
	 * @throws JaxenException
	 *             if the expression evauation failure
	 */

	private MessageContext getIteratedMessage(MessageContext synCtx,
	                                          SOAPEnvelope envelope, OMNode o)
	                                                                          throws AxisFault,
	                                                                          JaxenException {

		// clone the message for the mediation in iteration FIXME: is cloning
		// needed?
		MessageContext newCtx = MessageHelper.cloneMessageContext(synCtx);

		SOAPEnvelope newEnvelope = MessageHelper.cloneSOAPEnvelope(envelope);

		if (newEnvelope.getBody() != null) {

			if (newEnvelope.getBody().getFirstElement() != null) {
				newEnvelope.getBody().getFirstElement().detach();
			}
			newEnvelope.getBody().addChild(o);
		}

		// set the envelope and mediate as specified in the target
		newCtx.setEnvelope(newEnvelope);

		return newCtx;
	}

	public Target getTarget() {
		return target;
	}

	public void setTarget(Target target) {
		this.target = target;
	}

	public SynapsePath getExpression() {
		return expression;
	}

	public void setExpression(SynapsePath expression) {

		this.expression = expression;
	}

}
