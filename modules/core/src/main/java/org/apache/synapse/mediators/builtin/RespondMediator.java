package org.apache.synapse.mediators.builtin;


import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.flowtracer.MessageFlowDataHolder;
import org.apache.synapse.flowtracer.MessageFlowTracerConstants;
import org.apache.synapse.mediators.AbstractMediator;

public class RespondMediator extends AbstractMediator{

    public boolean mediate(MessageContext synCtx) {
        SynapseLog synLog = getLog(synCtx);
        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Respond Mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        setMediatorId();
        MessageFlowDataHolder.addEntry(synCtx, getMediatorId(), "Respond Mediator", true);
        synCtx.addComponentToMessageFlow(getMediatorId(), "Respond Mediator");

        synCtx.setTo(null);
        synCtx.setResponse(true);
        Axis2Sender.sendBack(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("End : Respond Mediator");
        }

        MessageFlowDataHolder.addEntry(synCtx, getMediatorId(), "Respond Mediator", false);

        return false;
    }

    @Override
    public boolean isContentAware() {
        return false;
    }
}
