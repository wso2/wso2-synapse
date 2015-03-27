package org.apache.synapse.endpoints;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.Pipe;
import org.apache.synapse.transport.passthru.util.RelayUtils;

import javax.xml.soap.SOAPEnvelope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FailoverEndpointOnHttpStatusCode can have multiple child endpoints. When http status code
 * support for a particular endpoint after recieving response if response error http status code
 * and the endpoint configured http status codes are matching the original request will send to the
 * next active endpoint. If next endpoint is not active then the request will be sent to a next available
 * endpoint if there is any. This procedure repeats until there are no active endpoints.
 */
public class FailoverEndpointOnHttpStatusCode extends FailoverEndpoint {

    /**
     * Last endpoint index
     */
    int index;

    public void send(MessageContext synCtx) {

        List<Endpoint> endpointList = null;

        /**Get endpoint list from MC*/
        if (synCtx.getProperty(SynapseConstants.ENDPOINT_LIST) != null) {
            endpointList = (List<Endpoint>) synCtx.getProperty(SynapseConstants.ENDPOINT_LIST);
        }

        Map<String, Integer> mEndpointLog = null;
        if (synCtx.getProperty(SynapseConstants.LAST_ENDPOINT) == null) {

            if (log.isDebugEnabled()) {
                log.debug(this + " Building the SoapEnvelope");
            }
            // If not yet a retry, we have to build the envelope since we need to support failover
            synCtx.getEnvelope().build();
            mEndpointLog = new HashMap<String, Integer>();
            synCtx.setProperty(SynapseConstants.ENDPOINT_LOG, mEndpointLog);
        } else {

            mEndpointLog = (Map<String, Integer>) synCtx.getProperty(SynapseConstants.ENDPOINT_LOG);
        }
        evaluateProperties(synCtx);


        /**To perform normal failover scenario looping through endpoints list*/
        int i = 1;
        while (i < endpointList.size()) {

            /**Get next endpoint in the list*/
            Endpoint nextEndpoint = endpointList.get(i);

            if (nextEndpoint.readyToSend()) {

                if (metricsMBean != null) {
                    metricsMBean.reportSendingFault(SynapseConstants.ENDPOINT_FO_FAIL_OVER);
                }

                synCtx.pushFaultHandler(this);
                if (nextEndpoint instanceof AbstractEndpoint) {
                    org.apache.axis2.context.MessageContext axisMC = ((Axis2MessageContext) synCtx).getAxis2MessageContext();

                    Pipe pipe = (Pipe) axisMC.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);

                    if (pipe != null) {
                        pipe.forceSetSerializationRest();
                    }
                    //allow the message to be content aware if the given message comes via PT
                    if (axisMC.getProperty(PassThroughConstants.PASS_THROUGH_PIPE) != null) {
                        ((AbstractEndpoint) nextEndpoint).setContentAware(true);
                        ((AbstractEndpoint) nextEndpoint).setForceBuildMC(true);

                        if (nextEndpoint instanceof TemplateEndpoint && ((TemplateEndpoint) nextEndpoint).getRealEndpoint() != null) {
                            if (((TemplateEndpoint) nextEndpoint).getRealEndpoint() instanceof AbstractEndpoint) {
                                ((AbstractEndpoint) ((TemplateEndpoint) nextEndpoint).getRealEndpoint()).setContentAware(true);
                                ((AbstractEndpoint) ((TemplateEndpoint) nextEndpoint).getRealEndpoint()).setForceBuildMC(true);
                            }
                        }
                    }
                }

                if (nextEndpoint.getName() != null) {

                    mEndpointLog.put(nextEndpoint.getName(), null);

                }

                if (log.isDebugEnabled()) {
                    log.debug(this + " Send message to the next endpoint");
                }

                synCtx.setProperty(SynapseConstants.CLONE_THIS_MSG, 0);

                /**Set the position of current endpoint*/
                synCtx.setProperty(SynapseConstants.ENDPOINT_INDEX, i);

                synCtx.setProperty(SynapseConstants.CLONED_SYN_MSG_CTX, synCtx);

                nextEndpoint.send(synCtx);
                break;
            }
            i++;
        }
    }
}
