/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.endpoints;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.AspectConfigurable;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.mediators.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * An Endpoint definition contains the information about an endpoint. It is used by leaf
 * level endpoints to store this information (e.g. AddressEndpoint and WSDLEndpoint).
 */
public class EndpointDefinition implements AspectConfigurable {

    private static final Log log = LogFactory.getLog(EndpointDefinition.class);

    public static final String DYNAMIC_URL_VALUE = "DYNAMIC_URL_VALUE";

    /** Who is the leaf level Endpoint which uses me? */
    public Endpoint leafEndpoint = null;
    /**
     * The simple address this endpoint resolves to - if explicitly specified
     */
    private String address = null;
    /**
     * Should messages be sent in an WS-RM Sequence ?
     */
    @Deprecated
    private boolean reliableMessagingOn = false;
    /**
     * Should messages be sent using WS-A?
     */
    private boolean addressingOn = false;
    /**
     * The addressing namespace version
     */
    private String addressingVersion = null;
    /**
     * Should messages be sent using WS-Security?
     */
    private boolean securityOn = false;
    /**
     * The "key" for any WS-RM Policy overrides to be used
     */
    @Deprecated
    private String wsRMPolicyKey = null;
    /**
     * The "key" for any Rampart Security Policy to be used
     */
    private String wsSecPolicyKey = null;
    /**
     * The "key" for any Rampart Security Policy to be used for inbound messages
     */
    private String inboundWsSecPolicyKey = null;
    /**
     * The "key" for any Rampart Security Policy to be used for outbound messages
     */
    private String outboundWsSecPolicyKey = null;
    /**
     * use a separate listener - implies addressing is on *
     */
    private boolean useSeparateListener = false;
    /**
     * force REST (POST) on *
     */
    private boolean forcePOX = false;
    /**
     * force REST (GET) on *
     */
    private boolean forceGET = false;
    /**
     * force SOAP11 on *
     */
    private boolean forceSOAP11 = false;
    /**
     * force SOAP11 on *
     */
    private boolean forceSOAP12 = false;
    /**
     * force REST on ?
     */
    private boolean forceREST = false;
    /**
     *  HTTP Endpoint
     */
    private boolean isHTTPEndpoint = false;
    /**
     * use MTOM *
     */
    private boolean useMTOM = false;
    /**
     * use SWA *
     */
    private boolean useSwa = false;
    /**
     * Endpoint message format. pox/soap11/soap12
     */
    private String format = null;

    /**
     * The charset encoding for messages sent to the endpoint.
     */
    private String charSetEncoding;

    /**
     * Whether endpoint state replication should be disabled or not (only valid in clustered setups)
     */
    private boolean replicationDisabled = false;

    /**
     * timeout duration for waiting for a response in ms. if the user has set some timeout action
     * and the timeout duration is not set, default is set to 0. note that if the user has
     * not set any timeout configuration, default timeout action is set to NONE, which won't do
     * anything for timeouts.
     */
    private final long defaultTimeoutDuration = 0;
    private Value timeoutDuration = new Value(String.valueOf(defaultTimeoutDuration));

    /**
     * Effective timeout interval for the endpoint
     */
    private long effectiveTimeout = 0;

    /**
     * action to perform when a timeout occurs (NONE | DISCARD | DISCARD_AND_FAULT) *
     */
    private final int defaultTimeoutAction = SynapseConstants.NONE;
    private Value timeoutAction = new Value("none");

    /** The initial suspend duration when an endpoint is marked inactive */
    private final long defaultInitialSuspendDuration = -1;
    private Value initialSuspendDuration = new Value(String.valueOf(defaultInitialSuspendDuration));

    /**
     * The suspend duration ratio for the next duration - this is the geometric series multipler
     */
    private final float defaultSuspendProgressionFactor = 1;
    private Value suspendProgressionFactor = new Value(String.valueOf(defaultSuspendProgressionFactor));

    /** This is the maximum duration for which a node will be suspended */
    private final long defaultSuspendMaximumDuration = Long.MAX_VALUE;
    private Value suspendMaximumDuration = new Value(String.valueOf(defaultSuspendMaximumDuration));

    /** A list of error codes, which directly puts an endpoint into suspend mode */
    private Value suspendErrorCodes = new Value("");

    /** No of retries to attempt on timeout, before an endpoint is makred inactive */
    private final int defaultRetriesOnTimeOutBeforeSuspend = 0;
    private Value retriesOnTimeoutBeforeSuspend = new Value(String.valueOf(defaultRetriesOnTimeOutBeforeSuspend));

    /** The delay between retries for a timeout out endpoint */
    private final int defaultRetryDurationOnTimeout = 0;
    private Value retryDurationOnTimeout = new Value(String.valueOf(defaultRetryDurationOnTimeout));

    /** A list of error codes which puts the endpoint into timeout mode */
    private Value timeoutErrorCodes = new Value("");

    private AspectConfiguration aspectConfiguration;

    /** A list of error codes which permit the retries */
    private final List<Integer> retryDisabledErrorCodes = new ArrayList<Integer>();

    /** A list of error codes which permit the retries for Enabled error Codes */
    private final List<Integer> retryEnabledErrorCodes = new ArrayList<Integer>();

    /** Variable to depict the effective timeout type **/
    private SynapseConstants.ENDPOINT_TIMEOUT_TYPE endpointTimeoutType;

    /** Expression to evaluate dynamic ws policy */
    private SynapsePath dynamicPolicy = null;

    public EndpointDefinition() {
        try {
            // Set the timeout value to global timeout value.
            // This will be overridden if endpoint timeout is set
            effectiveTimeout = SynapseConfigUtils.getGlobalTimeoutInterval();
            this.endpointTimeoutType = SynapseConstants.ENDPOINT_TIMEOUT_TYPE.GLOBAL_TIMEOUT;
        } catch (Exception ex) {
            String msg = "Error while reading global timeout interval";
            log.error(msg, ex);
            throw new SynapseException(msg, ex);
        }
    }

    public boolean isDynamicTimeoutEndpoint() {

        return timeoutDuration.getExpression() != null;
    }

    public long evaluateDynamicEndpointTimeout(MessageContext synCtx) {

        long timeoutMilliSeconds;
        try {
            String stringValue = timeoutDuration.evaluateValue(synCtx);
            if (stringValue != null) {
                timeoutMilliSeconds = Long.parseLong(stringValue.trim());
            } else {
                timeoutMilliSeconds = effectiveTimeout;
            }
        } catch (NumberFormatException e) {
            log.warn("Error while evaluating dynamic endpoint timeout expression." +
                    "Synapse global timeout is taken as effective timeout.");
            timeoutMilliSeconds = effectiveTimeout;
        }
        if (timeoutMilliSeconds > effectiveTimeout) {
            return effectiveTimeout;
        }
        return timeoutMilliSeconds;
    }

    /**
     * This should return the absolute EPR address referenced by the named endpoint. This may be
     * possibly computed.
     *
     * @return an absolute address to be used to reference the named endpoint
     */
    public String getAddress() {
        return address;
    }


    /**
     * This should return the absolute EPR address referenced by the named endpoint. This may be
     * possibly computed if the ${} properties specified in the URL.
     *
     * @param messageContext the current message context against the address is computed
     * @return an absolute address to be used to reference the named endpoint
     */
    public String getAddress(MessageContext messageContext) {
        if (address == null) {
            return null;
        }
        String addressString = address;
        String dynamicUrl = (String) messageContext.getProperty(DYNAMIC_URL_VALUE); // See ESBJAVA-3183.
        if (dynamicUrl != null && !dynamicUrl.isEmpty()) {
            addressString = dynamicUrl;
        }
        boolean matches = false;
        int s = 0;
        Pattern pattern = Pattern.compile("\\$\\{.*?\\}");

        StringBuffer computedAddress = new StringBuffer();

        Matcher matcher = pattern.matcher(addressString);
        while (matcher.find()) {


            Object property = messageContext.getProperty(
                    addressString.substring(matcher.start() + 2, matcher.end() - 1));
            if (property != null) {
                computedAddress.append(addressString.substring(s, matcher.start()));
                computedAddress.append(property.toString());
                s = matcher.end();
                matches = true;
            }
        }

        if (!matches) {
            return addressString;
        } else {
            computedAddress.append(addressString.substring(s, addressString.length()));
            return computedAddress.toString();
        }
    }

    /**
     * This should return the absolute EPR address referenced by the named endpoint. This method
     * is implemented to avoid the pattern matching computation in getAddress(MessageContext messageContext) method.
     *
     * @param messageContext the current message context against the address is computed
     * @return an absolute address to be used to reference the named endpoint
     */
    public String getDynamicAddress(MessageContext messageContext) {
        if (address == null) {
            return null;
        }
        String addressString = address;
        String dynamicUrl = (String) messageContext.getProperty(DYNAMIC_URL_VALUE);
        if (dynamicUrl != null && !dynamicUrl.isEmpty()) {
            addressString = dynamicUrl;
        }

        return addressString;
    }

    /**
     * Set an absolute URL as the address for this named endpoint
     *
     * @param address the absolute address to be used
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Is RM turned on on this endpoint?
     *
     * @return true if on
     */
    @Deprecated
    public boolean isReliableMessagingOn() {
        return reliableMessagingOn;
    }

    /**
     * Request that RM be turned on/off on this endpoint
     *
     * @param reliableMessagingOn a boolean flag indicating RM is on or not
     */
    @Deprecated
    public void setReliableMessagingOn(boolean reliableMessagingOn) {
        this.reliableMessagingOn = reliableMessagingOn;
    }

    /**
     * Is WS-A turned on on this endpoint?
     *
     * @return true if on
     */
    public boolean isAddressingOn() {
        return addressingOn;
    }

    /**
     * Request that WS-A be turned on/off on this endpoint
     *
     * @param addressingOn  a boolean flag indicating addressing is on or not
     */
    public void setAddressingOn(boolean addressingOn) {
        this.addressingOn = addressingOn;
    }

    /**
     * Get the addressing namespace version
     *
     * @return the adressing version
     */
    public String getAddressingVersion() {
        return addressingVersion;
    }

    /**
     * Set the addressing namespace version
     *
     * @param addressingVersion Version of the addressing spec to use
     */
    public void setAddressingVersion(String addressingVersion) {
        this.addressingVersion = addressingVersion;
    }

    /**
     * Is WS-Security turned on on this endpoint?
     *
     * @return true if on
     */
    public boolean isSecurityOn() {
        return securityOn;
    }

    /**
     * Request that WS-Sec be turned on/off on this endpoint
     *
     * @param securityOn  a boolean flag indicating security is on or not
     */
    public void setSecurityOn(boolean securityOn) {
        this.securityOn = securityOn;
    }

    /**
     * Return the Rampart Security configuration policys' 'key' to be used (See Rampart)
     *
     * @return the Rampart Security configuration policys' 'key' to be used (See Rampart)
     */
    public String getWsSecPolicyKey() {
        return wsSecPolicyKey;
    }

    /**
     * Set the Rampart Security configuration policys' 'key' to be used (See Rampart)
     *
     * @param wsSecPolicyKey the Rampart Security configuration policys' 'key' to be used
     */
    public void setWsSecPolicyKey(String wsSecPolicyKey) {
        this.wsSecPolicyKey = wsSecPolicyKey;
    }

    /**
     * Return the Rampart Security configuration policys' 'key' to be used for inbound messages
     * (See Rampart)
     *
     * @return the Rampart Security configuration policys' 'key' to be used for inbound messages
     */
    public String getInboundWsSecPolicyKey() {
        return inboundWsSecPolicyKey;
    }

    /**
     * Set the Rampart Security configuration policys' 'key' to be used for inbound messages
     * (See Rampart)
     *
     * @param inboundWsSecPolicyKey the Rampart Security configuration policys' 'key' to be used
     */
    public void setInboundWsSecPolicyKey(String inboundWsSecPolicyKey) {
        this.inboundWsSecPolicyKey = inboundWsSecPolicyKey;
    }

    /**
     * Return the Rampart Security configuration policys' 'key' to be used for outbound messages
     * (See Rampart)
     *
     * @return the ORampart Security configuration policys' 'key' to be used for outbound messages
     */
    public String getOutboundWsSecPolicyKey() {
        return outboundWsSecPolicyKey;
    }

    /**
     * Set the Rampart Security configuration policys' 'key' to be used (See Rampart)
     *
     * @param outboundWsSecPolicyKey the Rampart Security configuration policys' 'key' to be used
     */
    public void setOutboundWsSecPolicyKey(String outboundWsSecPolicyKey) {
        this.outboundWsSecPolicyKey = outboundWsSecPolicyKey;
    }

    /**
     * Get the WS-RM configuration policys' 'key' to be used
     *
     * @return the WS-RM configuration policys' 'key' to be used
     */
    @Deprecated
    public String getWsRMPolicyKey() {
        return wsRMPolicyKey;
    }

    /**
     * Set the WS-RM configuration policys' 'key' to be used
     *
     * @param wsRMPolicyKey the WS-RM configuration policys' 'key' to be used
     */
    @Deprecated
    public void setWsRMPolicyKey(String wsRMPolicyKey) {
        this.wsRMPolicyKey = wsRMPolicyKey;
    }

    public void setUseSeparateListener(boolean b) {
        this.useSeparateListener = b;
    }

    public boolean isUseSeparateListener() {
        return useSeparateListener;
    }

    public void setForcePOX(boolean forcePOX) {
        this.forcePOX = forcePOX;
    }

    public boolean isForcePOX() {
        return forcePOX;
    }

    public boolean isForceGET() {
        return forceGET;
    }

    public void setForceGET(boolean forceGET) {
        this.forceGET = forceGET;
    }

    public void setForceSOAP11(boolean forceSOAP11) {
        this.forceSOAP11 = forceSOAP11;
    }

    public boolean isForceSOAP11() {
        return forceSOAP11;
    }

    public void setForceSOAP12(boolean forceSOAP12) {
        this.forceSOAP12 = forceSOAP12;
    }

    public boolean isForceSOAP12() {
        return forceSOAP12;
    }

    public boolean isForceREST() {
        return forceREST;
    }

    public void setForceREST(boolean forceREST) {
        this.forceREST = forceREST;
    }

    public boolean isUseMTOM() {
        return useMTOM;
    }

    public void setUseMTOM(boolean useMTOM) {
        this.useMTOM = useMTOM;
    }

    public boolean isUseSwa() {
        return useSwa;
    }

    public void setUseSwa(boolean useSwa) {
        this.useSwa = useSwa;
    }

    public String getTimeoutDuration() {

        if (timeoutDuration.getKeyValue() != null) {
            return timeoutDuration.getKeyValue();
        }
        return timeoutDuration.getExpression().getExpression();
    }

    /**
     * Get the effective timeout duration for the endpoint
     *
     * If endpoint timeout is set explicitly this will return that,
     * If not global timeout interval is returned
     *
     * @return effective timeout duration for the endpoint
     */
    public long getEffectiveTimeout() {
        return effectiveTimeout;
    }

    /**
     * Set the timeout duration.
     *
     * @param timeoutDuration a duration in milliseconds
     */
    public void setTimeoutDuration(Value timeoutDuration) {

        this.timeoutDuration = timeoutDuration;
        if (timeoutDuration.getKeyValue() != null) {
            this.effectiveTimeout = Long.parseLong(timeoutDuration.getKeyValue());
        }
        this.endpointTimeoutType = SynapseConstants.ENDPOINT_TIMEOUT_TYPE.ENDPOINT_TIMEOUT;
    }

    public String getTimeoutAction() {

        if (timeoutAction.getKeyValue() != null) {
            return timeoutAction.getKeyValue();
        }
        return timeoutAction.getExpression().getExpression();
    }

    public void setTimeoutAction(Value timeoutAction) {

        this.timeoutAction = timeoutAction;
    }

    public boolean isTimeoutActionDynamic() {

        return timeoutAction.getExpression() != null;
    }

    public int getResolvedTimeoutAction(MessageContext synCtx) {

        int result = defaultTimeoutAction;
        String timeoutActionStr = timeoutAction.evaluateValue(synCtx);
        if (timeoutActionStr != null) {
            if (EPConstants.DISCARD.equalsIgnoreCase(timeoutActionStr)) {
                result = SynapseConstants.DISCARD;
            } else if (EPConstants.FAULT.equalsIgnoreCase(timeoutActionStr)) {
                result = SynapseConstants.DISCARD_AND_FAULT;
            } else {
                log.warn("Invalid timeout action, action : '" + timeoutActionStr + "' is not supported.");
            }
        }
        return result;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Get the charset encoding for messages sent to the endpoint.
     *
     * @return charSetEncoding
     */
    public String getCharSetEncoding() {
        return charSetEncoding;
    }

    /**
     * Set the charset encoding for messages sent to the endpoint.
     *
     * @param charSetEncoding the charset encoding or <code>null</code>
     */
    public void setCharSetEncoding(String charSetEncoding) {
        this.charSetEncoding = charSetEncoding;
    }

    /**
     * Get the suspend on fail duration.
     *
     * @return suspendOnFailDuration
     */
    public String getInitialSuspendDuration() {

        if (initialSuspendDuration.getKeyValue() != null) {
            return initialSuspendDuration.getKeyValue();
        }
        return initialSuspendDuration.getExpression().getExpression();
    }

    /**
     * Set the suspend on fail duration.
     *
     * @param initialSuspendDuration a duration in milliseconds
     */
    public void setInitialSuspendDuration(Value initialSuspendDuration) {

        this.initialSuspendDuration = initialSuspendDuration;
    }

    public boolean isInitialSuspendDurationDynamic() {

        return initialSuspendDuration.getExpression() != null;
    }

    public long getResolvedInitialSuspendDuration(MessageContext synCtx) {

        long result = defaultInitialSuspendDuration;
        String stringValue = "";
        try {
            stringValue = initialSuspendDuration.evaluateValue(synCtx);
            if (stringValue != null) {
                result = Long.parseLong(stringValue.trim());
            }
        } catch (NumberFormatException e) {
            log.warn("Error while evaluating initial suspend duration. The resolved value '" + stringValue + "' should be a valid number. Hence the default value '" + defaultInitialSuspendDuration + "' is used.");
        }
        return result;
    }

    public String getSuspendProgressionFactor() {

        if (suspendProgressionFactor.getKeyValue() != null) {
            return suspendProgressionFactor.getKeyValue();
        }
        return suspendProgressionFactor.getExpression().getExpression();
    }

    public void setSuspendProgressionFactor(Value suspendProgressionFactor) {

        this.suspendProgressionFactor = suspendProgressionFactor;
    }

    public boolean isSuspendProgressionFactorDynamic() {

        return suspendProgressionFactor.getExpression() != null;
    }

    public float getResolvedSuspendProgressionFactor(MessageContext messageContext) {

        float result = defaultSuspendProgressionFactor;
        String stringValue = "";
        try {
            stringValue = suspendProgressionFactor.evaluateValue(messageContext);
            if (stringValue != null) {
                result = Float.parseFloat(stringValue);
            }
        } catch (NumberFormatException e) {
            log.warn("Error while evaluating suspend duration progression factor. The resolved value '" + stringValue + "' should be a valid float. Hence the default value '" + defaultSuspendProgressionFactor + "' is used.");
        }
        return result;
    }

    public String getSuspendMaximumDuration() {

        if (suspendMaximumDuration.getKeyValue() != null) {
            return suspendMaximumDuration.getKeyValue();
        }
        return suspendMaximumDuration.getExpression().getExpression();
    }

    public void setSuspendMaximumDuration(Value suspendMaximumDuration) {

        this.suspendMaximumDuration = suspendMaximumDuration;
    }

    public boolean isSuspendMaximumDurationDynamic() {

        return suspendMaximumDuration.getExpression() != null;
    }

    public long getResolvedSuspendMaximumDuration(MessageContext messageContext) {

        long result = defaultSuspendMaximumDuration;
        String stringValue = "";
        try {
            stringValue = suspendMaximumDuration.evaluateValue(messageContext);
            if (stringValue != null) {
                result = Long.parseLong(stringValue);
            }
        } catch (NumberFormatException e) {
            log.warn("Error while evaluating suspend maximum duration. The resolved value '" + stringValue + "' should be a valid number. Hence the default value '" + defaultSuspendMaximumDuration + "' is used.");
        }
        return result;
    }

    public String getRetriesOnTimeoutBeforeSuspend() {

        if (retriesOnTimeoutBeforeSuspend.getKeyValue() != null) {
            return retriesOnTimeoutBeforeSuspend.getKeyValue();
        }
        return retriesOnTimeoutBeforeSuspend.getExpression().getExpression();
    }

    public void setRetriesOnTimeoutBeforeSuspend(Value retriesOnTimeoutBeforeSuspend) {

        this.retriesOnTimeoutBeforeSuspend = retriesOnTimeoutBeforeSuspend;
    }

    public boolean isRetriesOnTimeoutBeforeSuspendDynamic() {

        return retriesOnTimeoutBeforeSuspend.getExpression() != null;
    }

    public int getResolvedRetriesOnTimeoutBeforeSuspend(MessageContext messageContext) {

        int result = defaultRetriesOnTimeOutBeforeSuspend;
        String stringValue = "";
        try {
            stringValue = retriesOnTimeoutBeforeSuspend.evaluateValue(messageContext);
            if (stringValue != null) {
                result = Integer.parseInt(stringValue);
            }
        } catch (NumberFormatException e) {
            log.warn("Error while evaluating retries before suspend [for timeouts]. The resolved value '" + stringValue + "' should be a valid number. Hence the default value '" + defaultRetriesOnTimeOutBeforeSuspend + "' is used.");
        }
        return result;
    }

    public String getRetryDurationOnTimeout() {

        if (retryDurationOnTimeout.getKeyValue() != null) {
            return retryDurationOnTimeout.getKeyValue();
        }
        return retryDurationOnTimeout.getExpression().getExpression();
    }

    public void setRetryDurationOnTimeout(Value retryDurationOnTimeout) {

        this.retryDurationOnTimeout = retryDurationOnTimeout;
    }

    public boolean isRetryDurationOnTimeoutDynamic() {

        return retryDurationOnTimeout.getExpression() != null;
    }

    public int getResolvedRetryDurationOnTimeout(MessageContext messageContext) {

        int result = defaultRetryDurationOnTimeout;
        String stringValue = "";
        try {
            stringValue = retryDurationOnTimeout.evaluateValue(messageContext);
            if (stringValue != null) {
                result = Integer.parseInt(stringValue);
            }
        } catch (NumberFormatException e) {
            log.warn("Error while evaluating retry delay for timeouts. The resolved value '" + stringValue + "' should be a valid number. Hence the default value '" + defaultRetryDurationOnTimeout + "' is used.");
        }
        return result;
    }

    public String getSuspendErrorCodes() {

        if (suspendErrorCodes.getKeyValue() != null) {
            return suspendErrorCodes.getKeyValue();
        }
        return suspendErrorCodes.getExpression().getExpression();
    }

    public boolean isSuspendErrorCodesDynamic() {

        return suspendErrorCodes.getExpression() != null;
    }

    public String getTimeoutErrorCodes() {

        if (timeoutErrorCodes.getKeyValue() != null) {
            return timeoutErrorCodes.getKeyValue();
        }
        return timeoutErrorCodes.getExpression().getExpression();
    }

    public List<Integer> getRetryDisabledErrorCodes() {
        return retryDisabledErrorCodes;
    }

    public List<Integer> getRetryEnableErrorCodes() {
        return retryEnabledErrorCodes;
    }

    public boolean isReplicationDisabled() {
        return replicationDisabled;
    }

    public void setReplicationDisabled(boolean replicationDisabled) {
        this.replicationDisabled = replicationDisabled;
    }

    public void setSuspendErrorCodes(Value suspendErrorCodes) {

        this.suspendErrorCodes = suspendErrorCodes;
    }

    public void addSuspendErrorCode(int code) {

        if (suspendErrorCodes.getKeyValue() != null) {
            suspendErrorCodes = new Value(suspendErrorCodes.getKeyValue() + "," + code);
        }
    }

    public List<Integer> getResolvedSuspendErrorCodes(MessageContext messageContext) {

        List<Integer> result = new ArrayList<>();
        String stringValue = "";
        try {
            stringValue = suspendErrorCodes.evaluateValue(messageContext);
            if (stringValue != null) {
                String[] errorCodes = stringValue.split(",");
                result = new ArrayList<Integer>();
                for (String errorCode : errorCodes) {
                    result.add(Integer.parseInt(errorCode.trim()));
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Error while evaluating suspend error codes. The resolved value '" + stringValue + "' should be valid numbers separated by commas.");
        }
        return result;
    }

    public void setTimeoutErrorCodes(Value timeoutErrorCodes) {

        this.timeoutErrorCodes = timeoutErrorCodes;
    }

    public void addTimeoutErrorCode(int code) {

        if (timeoutErrorCodes.getKeyValue() != null) {
            timeoutErrorCodes = new Value(timeoutErrorCodes.getKeyValue() + "," + code);
        }
    }

    public boolean isTimeoutErrorCodesDynamic() {

        return timeoutErrorCodes.getExpression() != null;
    }

    public List<Integer> getResolvedTimeoutErrorCodes(MessageContext messageContext) {

        List<Integer> result = new ArrayList<>();
        String stringValue = "";
        try {
            stringValue = timeoutErrorCodes.evaluateValue(messageContext);
            if (stringValue != null) {
                String[] errorCodes = stringValue.split(",");
                result = new ArrayList<Integer>();
                for (String errorCode : errorCodes) {
                    result.add(Integer.parseInt(errorCode.trim()));
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Error while evaluating timeout error codes. The resolved value '" + stringValue + "' should be valid numbers separated by commas.");
        }
        return result;
    }

    public void addRetryDisabledErrorCode(int code) {
        retryDisabledErrorCodes.add(code);
    }
    public void addRetryEnabledErrorCode(int code) {
        retryEnabledErrorCodes.add(code);
    }

    public boolean isHTTPEndpoint() {
        return isHTTPEndpoint;
    }

    public void setHTTPEndpoint(boolean HTTPEndpoint) {
        isHTTPEndpoint = HTTPEndpoint;
    }

    public String toString() {
        if (leafEndpoint != null) {
            return leafEndpoint.toString();
        } else if (address != null) {
            return "Address [" + address + "]";
        }
        return "[unknown endpoint]";
    }

    public void setLeafEndpoint(Endpoint leafEndpoint) {
        this.leafEndpoint = leafEndpoint;
    }

    public boolean isStatisticsEnable() {
        return this.aspectConfiguration != null && this.aspectConfiguration.isStatisticsEnable();
    }

    public void disableStatistics() {
        if (this.aspectConfiguration != null) {
            this.aspectConfiguration.disableStatistics();
        }
    }

    public void enableStatistics() {
        if (this.aspectConfiguration != null) {
            this.aspectConfiguration.enableStatistics();
        }
    }

    public boolean isTracingEnabled() {
        return this.aspectConfiguration != null && this.aspectConfiguration.isTracingEnabled();
    }

    public void disableTracing() {
        if (this.aspectConfiguration != null) {
            this.aspectConfiguration.disableTracing();
        }
    }

    public void enableTracing() {
        if (this.aspectConfiguration != null) {
            this.aspectConfiguration.enableTracing();
        }
    }

    public void configure(AspectConfiguration aspectConfiguration) {
        this.aspectConfiguration = aspectConfiguration;
    }

    public AspectConfiguration getAspectConfiguration() {
        return this.aspectConfiguration;
    }

    public SynapseConstants.ENDPOINT_TIMEOUT_TYPE getEndpointTimeoutType() {
        return endpointTimeoutType;
    }

    public void setEndpointTimeoutType(SynapseConstants.ENDPOINT_TIMEOUT_TYPE endpointTimeoutType) {
        this.endpointTimeoutType = endpointTimeoutType;
    }

    /**
     * GET expression to evaluate dynamic ws policy
     *
     * @return SynapsePath to the policy
     */
    public SynapsePath getDynamicPolicy() {
        return dynamicPolicy;
    }

    /**
     * Set expression to evaluate dynamic ws policy
     *
     * @param dynamicPolicy SynapsePath to the policy
     */
    public void setDynamicPolicy(SynapsePath dynamicPolicy) {
        this.dynamicPolicy = dynamicPolicy;
    }

    /**
     * Checks ws security policy is a dynamic or static one
     *
     * @return true if policy is dynamic else false.
     */
    public boolean isDynamicPolicy() {
        if (this.dynamicPolicy != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Evaluates the ws security policy path dynamically
     *
     * @param synCtx MessageContext
     * @return string value of policy path
     */
    public String evaluateDynamicEndpointSecurityPolicy(MessageContext synCtx) {
        String wsSePolicy;
        String stringValue = dynamicPolicy.stringValueOf(synCtx);
        if (stringValue != null) {
            wsSePolicy = stringValue.trim();
        } else {
            log.warn("Error while evaluating dynamic endpoint timeout expression."+
                    "Synapse global timeout is taken as effective timeout.");
            wsSePolicy = wsSecPolicyKey;
        }
        return wsSePolicy;
    }


}
