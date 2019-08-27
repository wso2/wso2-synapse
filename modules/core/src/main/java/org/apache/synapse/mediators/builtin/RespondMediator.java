package org.apache.synapse.mediators.builtin;


import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.rest.RESTUtils;

public class RespondMediator extends AbstractMediator{

    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);
        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Respond Mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        synCtx.setTo(null);
        synCtx.setResponse(true);

        // if this is not a response from a proxy service
        String proxyName = (String) synCtx.getProperty(SynapseConstants.PROXY_SERVICE);
        if (proxyName == null || proxyName.isEmpty()) {
            // Add CORS headers for API response
            RESTUtils.handleCORSHeadersForResponse(synCtx);
        }

        Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
        org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
        axis2MessageCtx.getOperationContext()
                       .setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN, "SKIP");
        Axis2Sender.sendBack(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("End : Respond Mediator");
        }

        return false;
    }

    @Override
    public boolean isContentAware() {
        return false;
    }
}
