package org.apache.synapse.mediators.builtin;


import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.collector.CollectorEnabler;
import org.apache.synapse.mediators.collector.MediatorData;
import org.apache.synapse.mediators.collector.TreeNode;

public class RespondMediator extends AbstractMediator{

    public boolean mediate(MessageContext synCtx) {
        SynapseLog synLog = getLog(synCtx);
        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Respond Mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        synCtx.setTo(null);
        synCtx.setResponse(true);
        Axis2Sender.sendBack(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("End : Respond Mediator");
        }
        if (CollectorEnabler.checkCollectorRequired()) {
        	// In case of arespond mediator since the further execution is halt,
        	// publish the current tree to the list
        	MediatorData.setEndingTime(synCtx.getCurrent().getLastChild());

        	MediatorData.toTheList((TreeNode) synCtx
        			.getProperty("NonFaultRoot"));

        }
        return false;
    }

    @Override
    public boolean isContentAware() {
        return false;
    }
}
