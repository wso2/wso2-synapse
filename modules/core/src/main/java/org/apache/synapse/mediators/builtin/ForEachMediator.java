package org.apache.synapse.mediators.builtin;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.xpath.SynapseXPath;


public class ForEachMediator extends AbstractMediator {

	 /** The XPath that will list the elements to be splitted */
    private SynapseXPath expression = null;
    
	public boolean mediate(MessageContext synCtx) {
		SynapseLog synLog = getLog(synCtx);
		synLog.traceOrDebug("Start : For Each mediator");
		if(expression!=null){
		synLog.traceOrDebug("ForEach: expression = " + expression.toString());
		}
		else{
			synLog.traceOrDebug("ForEach: expression is null");
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
