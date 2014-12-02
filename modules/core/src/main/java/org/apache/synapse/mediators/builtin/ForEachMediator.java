package org.apache.synapse.mediators.builtin;

import java.util.List;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

public class ForEachMediator extends AbstractMediator {

	/** The XPath that will list the elements to be splitted */
	private SynapseXPath expression = null;

	public boolean mediate(MessageContext synCtx) {
		SynapseLog synLog = getLog(synCtx);
		
		if (synLog.isTraceOrDebugEnabled()) {
			synLog.traceOrDebug("Start : Foreach mediator");
			synLog.traceTrace("Message : " + synCtx.getEnvelope());

			if (synLog.isTraceTraceEnabled()) {
				synLog.traceTrace("Message : " + synCtx.getEnvelope());
			}
		}
		
		if (expression != null) {
			synLog.traceOrDebug("ForEach: expression = " + expression.toString());
		} else {
			synLog.traceOrDebug("ForEach: expression is null");
		}
		
		
            try {
	            // get a copy of the message for the processing, if the continueParent is set to true
	            // this original message can go in further mediations and hence we should not change
	            // the original message context
	            SOAPEnvelope envelope = MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope());
	            // get the iteration elements and iterate through the list,
	            // this call will also detach all the iteration elements 
	            List splitElements =
	                                 EIPUtils.getDetachedMatchingElements(envelope, synCtx,
	                                                                      expression);
	            if (synLog.isTraceOrDebugEnabled()) {
		            synLog.traceOrDebug("Splitting with XPath : " + expression + " resulted in " +
		                                splitElements.size() + " elements");
	            }
            } catch  (JaxenException e) {
                handleException("Error evaluating split XPath expression : " + expression, e, synCtx);
            }
		synLog.traceOrDebug("End : For Each mediator");
		return true;
	}

	public SynapseXPath getExpression() {
		return expression;
	}

	public void setExpression(SynapseXPath expression) {
		this.expression = expression;
	}

}
