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

import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.List;

public class ForEachMediator extends AbstractMediator {

	/**
	 * The xpath that will list the elements to be split
	 */
	private SynapseXPath expression = null;

	/**
	 * The target for the newly split messages
	 */
	private Target target = null;

	public boolean mediate(MessageContext synCtx) {
		SynapseLog synLog = getLog(synCtx);

		if (synLog.isTraceOrDebugEnabled()) {
			synLog.traceOrDebug("Start : Foreach mediator");
		}

		if (expression == null) {
			synLog.error("ForEach: expression is null");
			return false;
		} else {
			synLog.traceOrDebug("ForEach: expression = " +
			                    expression.toString());

			if (!validateTarget(synCtx)) {
				synLog.error(
						"ForEach: Target cannot contain an endpoint OR Sequence is invalid/null :: "
						+ "cannot contain Call, Send or CallOut mediators");
				return false;
			} else {

				if (synLog.isTraceOrDebugEnabled()) {
					synLog.traceOrDebug("Splitting with Xpath : " +
					                    expression);

				}
				try {

					SOAPEnvelope envelope = synCtx.getEnvelope();
					// get the iteration elements and iterate through the list, this call
					// will also detach all the iteration elements from the message
					List<?> splitElements =
							EIPUtils.getDetachedMatchingElements(envelope,
							                                     synCtx,
							                                     (SynapseXPath) expression);

					int msgCount = splitElements.size();
					int msgNumber = 0;

					if (synLog.isTraceOrDebugEnabled()) {
						synLog.traceOrDebug("Splitting with XPath : " +
						                    expression + " resulted in " +
						                    msgCount + " elements");
					}

					// iterate through the list
					for (Object element : splitElements) {
						if (!(element instanceof OMNode)) {
							handleException("Error splitting message with XPath : " +
							                expression +
							                " - result not an OMNode",
							                synCtx);
						}

						if (synLog.isTraceOrDebugEnabled()) {
							synLog.traceOrDebug("Submitting " + msgNumber +
							                    " of " + msgCount +
							                    " messages for processing in sequentially, in a general loop");
						}

						MessageContext iteratedMsgCtx =
								getIteratedMessage(synCtx,
								                   envelope,
								                   (OMNode) element);

						target.mediate(iteratedMsgCtx);
						EIPUtils.includeEnvelope(envelope,
						                         iteratedMsgCtx.getEnvelope(),
						                         synCtx,
						                         (SynapseXPath) expression);
						synCtx.setEnvelope(envelope);
					}

				} catch (JaxenException e) {
					handleException("Error evaluating split XPath expression : " +
					                expression, e, synCtx);
				} catch (AxisFault af) {
					handleException("Error creating an iterated copy of the message",
					                af, synCtx);
				}
				if (synLog.isTraceOrDebugEnabled()) {
					synLog.traceOrDebug("End : For Each mediator");
				}
				return true;

			}
		}
	}

	/**
	 * Validate at runtime tha the Target of the ForEach mediator. The target cannot contain :
	 * <ul>
	 * <li>A sequence with Call, CallOut or Send Mediators</li>
	 * <li>A reference to a sequence that contains Call, CallOut or Send Mediators </li>
	 * <li>A target endpoint</li>
	 * <li>A reference to a target endpoint</li>
	 * </ul>
	 *
	 * @param synCtx Message Context being mediated
	 * @return validity of the sequence
	 */
	private boolean validateTarget(MessageContext synCtx) {

		SequenceMediator sequence = target.getSequence();
		boolean valid = true;
		if (sequence != null) {
			valid = validateSequenceMediatorList(sequence);
		} else {
			String sequenceRef = target.getSequenceRef();
			if (sequenceRef != null) {
				SequenceMediator refSequence =
						(SequenceMediator) synCtx.getSequence(sequenceRef);

				if (refSequence != null) {
					valid = validateSequenceMediatorList(refSequence);
				} else {
					valid = false;
				}
			} else if ((target.getEndpoint() != null) ||
			           (target.getEndpointRef() != null)) {
				valid = false;
			} else {
				valid = false;
			}
		}
		return valid;

	}

	/**
	 * Validate a sequence mediator to not contain Call, CallOut or Send mediators in it.
	 *
	 * @param seqMediator Sequence Mediator to validate
	 * @return validity of the sequence mediator
	 */
	private boolean validateSequenceMediatorList(SequenceMediator seqMediator) {
		boolean valid = true;
		List<Mediator> mediators = seqMediator.getList();
		for (Mediator m : mediators) {
			if (m instanceof CallMediator) {
				valid = false;
				break;
			} else if (m instanceof CalloutMediator) {
				valid = false;
				break;
			} else if (m instanceof SendMediator) {
				valid = false;
				break;
			}
		}
		return valid;
	}

	/**
	 * Create a new message context using the given original message context,
	 * the envelope
	 * and the split result element.
	 *
	 * @param synCtx   - original message context
	 * @param envelope - envelope to be used in the iteration
	 * @param omNode   - element which participates in the iteration replacement
	 * @return new Message Context created by the iteration
	 * @throws AxisFault      if there is a message creation failure
	 * @throws JaxenException if the expression evaluation failure
	 */
	private MessageContext getIteratedMessage(MessageContext synCtx,
	                                          SOAPEnvelope envelope, OMNode omNode)
			throws AxisFault,
			       JaxenException {

		MessageContext newCtx = MessageHelper.cloneMessageContext(synCtx);

		SOAPEnvelope newEnvelope = MessageHelper.cloneSOAPEnvelope(envelope);

		if (newEnvelope.getBody() != null) {

			if (newEnvelope.getBody().getFirstElement() != null) {
				newEnvelope.getBody().getFirstElement().detach();
			}
			newEnvelope.getBody().addChild(omNode);
		}

		newCtx.setEnvelope(newEnvelope);

		return newCtx;
	}

	public Target getTarget() {
		return target;
	}

	public void setTarget(Target target) {
		this.target = target;
	}

	public SynapseXPath getExpression() {
		return expression;
	}

	public void setExpression(SynapseXPath expression) {
		this.expression = expression;
	}

}
