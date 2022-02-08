package org.apache.synapse.mediators.opa;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.util.RelayUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class OPAMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(OPAMediator.class);

    private String serverUrl = null;
    private String accessToken = null;
    private String policy = null;
    private String rule = null;
    private String requestGeneratorClassName = "org.apache.synapse.mediators.opa.OPASynapseRequestGenerator";
    private Map<String, Object> advancedProperties = new HashMap<String, Object>();

    public void init() {

    }

    @Override
    public boolean mediate(MessageContext messageContext) {

        OPARequestGenerator requestGenerator = null;
        String opaResponseString = null;
        try {
            try {
                Class<?> requestGeneratorClassObject = Class.forName(requestGeneratorClassName);
                Constructor<?> constructor = requestGeneratorClassObject.getConstructor();
                requestGenerator = (OPARequestGenerator) constructor.newInstance();
            } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException
                    | InvocationTargetException e) {
                throw new OPASecurityException(OPASecurityException.MEDIATOR_ERROR,
                        "Cannot initialize the provided request generator", e);
            }

            String opaPayload = requestGenerator.createRequest(policy, rule, advancedProperties, messageContext);
            String evaluatingPolicyUrl = serverUrl + "/" + policy + "/" + rule;

            opaResponseString = OPAClient.publish(evaluatingPolicyUrl, opaPayload, accessToken);
            return requestGenerator.handleResponse(policy, rule, opaResponseString, messageContext);
        } catch (OPASecurityException e) {
            handleAuthFailure(messageContext, e);
        }
        return false;
    }

    protected void handleAuthFailure(MessageContext messageContext, OPASecurityException e) {

        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();
        // This property need to be set to avoid sending the content in pass-through pipe (request message)
        // as the response.
        axis2MC.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
        try {
            RelayUtils.consumeAndDiscardMessage(axis2MC);
        } catch (AxisFault axisFault) {
            //In case of an error it is logged and the process is continued because we're setting a fault message in the payload.
            log.error("Error occurred while consuming and discarding the message", axisFault);
        }
        axis2MC.setProperty(Constants.Configuration.MESSAGE_TYPE, "application/soap+xml");
        int status;
        String errorMessage;
        if (e.getErrorCode() == OPASecurityException.MEDIATOR_ERROR
                || e.getErrorCode() == OPASecurityException.OPA_REQUEST_ERROR) {
            status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            errorMessage = "Internal Sever Error";
        } else if (e.getErrorCode() == OPASecurityException.ACCESS_REVOKED) {
            status = HttpStatus.SC_FORBIDDEN;
            errorMessage = "Forbidden";
        } else if (e.getErrorCode() == OPASecurityException.OPA_RESPONSE_ERROR) {
            status = HttpStatus.SC_BAD_REQUEST;
            errorMessage = "Bad Request";
        } else {
            status = HttpStatus.SC_UNAUTHORIZED;
            errorMessage = "Unauthorized";
        }

        messageContext.setProperty(SynapseConstants.ERROR_CODE, status);
        messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, errorMessage);
        messageContext.setProperty(SynapseConstants.ERROR_EXCEPTION, e);
        OPAUtils.sendFault(messageContext, status);
    }

    public String getServerUrl() {

        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {

        this.serverUrl = serverUrl;
    }

    public String getAccessToken() {

        return accessToken;
    }

    public void setAccessToken(String accessToken) {

        this.accessToken = accessToken;
    }

    public String getRequestGeneratorClassName() {

        return requestGeneratorClassName;
    }

    public void setRequestGeneratorClassName(String requestGeneratorClassName) {

        this.requestGeneratorClassName = requestGeneratorClassName;
    }

    public String getPolicy() {

        return policy;
    }

    public void setPolicy(String policy) {

        this.policy = policy;
    }

    public String getRule() {

        return rule;
    }

    public void setRule(String rule) {

        this.rule = rule;
    }

    public Map<String, Object> getAdvancedProperties() {

        return advancedProperties;
    }

    public void setAdvancedProperties(Map<String, Object> advancedProperties) {

        this.advancedProperties = advancedProperties;
    }

    public void addAdvancedProperty(String property, Object value) {

        this.advancedProperties.put(property, value);
    }
}
