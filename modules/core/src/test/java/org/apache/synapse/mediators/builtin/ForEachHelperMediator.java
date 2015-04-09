package org.apache.synapse.mediators.builtin;

import org.apache.axis2.AxisFault;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.MessageHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper mediator class to test ForEach mediator
 *
 */
public class ForEachHelperMediator extends AbstractMediator implements ManagedLifecycle {

	private List mediatedContext = new ArrayList();
	private int msgcount;
	
	public boolean mediate(MessageContext synCtx) {
		
		try {
	        mediatedContext.add(MessageHelper.cloneMessageContext(synCtx));
        } catch (AxisFault e) {
	        e.printStackTrace();
        }
		msgcount++;
		return false;
	}

	public MessageContext getMediatedContext(int position) {
		if (mediatedContext.size() > position) {
			return (MessageContext) mediatedContext.get(position);
		} else {
			return null;
		}
	}

	public void clearMediatedContexts() {
		mediatedContext.clear();
		msgcount = 0;
	}


	public void init(SynapseEnvironment se) {
		msgcount = 0;
	}

	public int getMsgCount(){
		return msgcount;
	}
	
	public void destroy() {
		clearMediatedContexts();
		msgcount = 0;
	}
}
