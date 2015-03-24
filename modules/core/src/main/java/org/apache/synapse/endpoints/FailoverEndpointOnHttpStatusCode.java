package org.apache.synapse.endpoints;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.Pipe;

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
     * All configured endpoints
     */
    List<Endpoint> endpointList = null;
    /**
     * Last endpoint index
     */
    int index;

    public void send(MessageContext synCtx) {

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

                synCtx.setProperty("INDEX", i);


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

                /**If next endpoint support http status codes we need to clone the message*/
                if(((AbstractEndpoint)nextEndpoint).getDefinition().getFailoverHttpstatusCodes()!=null){
                    synCtx.setProperty(SynapseConstants.CLONE_THIS_MSG, 1);
                }
                else{
                    synCtx.setProperty(SynapseConstants.CLONE_THIS_MSG, 0);
                }

                nextEndpoint.send(synCtx);
                break;
            }
            i++;
        }
    }

    /**
     * Everytime a response message is received this method gets invoked if the response http status code is
     * larger than 500 and failover enabled according the http status in the message context.
     * the incoming Synapse message context for the reply we received, and determine what action
     * to take according to the synapse configurations of the endpoint and the http status code
     * of the response
     *
     * @param msgHttpStatus the response http status code
     * @param synMC         response Synapse message context
     */

    public void resend(String msgHttpStatus, MessageContext synMC) {

        /**Get last endpoint index from MC*/
        if (synMC.getProperty(SynapseConstants.ENDPOINT_INDEX) != null) {
            index = (Integer) synMC.getProperty(SynapseConstants.ENDPOINT_INDEX);
        }
        /**Get endpoint list from MC*/
        if (synMC.getProperty(SynapseConstants.ENDPOINT_LIST) != null) {
            endpointList = (List<Endpoint>) synMC.getProperty(SynapseConstants.ENDPOINT_LIST);
        }

        /**Check whether there is a next endpoint*/
        if (endpointList.size() > 1) {

            /**Last endpoint*/
            Endpoint lastEndpoint = endpointList.get(index);

            /**Get http status of the endpoint*/
            List<Integer> lastEndpointHttpStatus = ((AbstractEndpoint)lastEndpoint).getDefinition().getFailoverHttpstatusCodes();

            if(!lastEndpointHttpStatus.isEmpty()) {
                for (int endpointHttpStatus : lastEndpointHttpStatus) {
                    /**If msg http status and endpoint http status matching send*/
                    if (Integer.parseInt(msgHttpStatus) == endpointHttpStatus) {
                        if (log.isDebugEnabled()) {
                            log.debug(this + " Calling send message");
                        }
                        /**Send message if last endpoint configured for http status codes*/
                        this.send(synMC);
                        break;
                    }
                }
            }else{
                if (log.isDebugEnabled()) {
                    log.debug(this + " No failover status codes for the endpoint");
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(this + " There is no next endpoint");
            }
        }

    }
}
