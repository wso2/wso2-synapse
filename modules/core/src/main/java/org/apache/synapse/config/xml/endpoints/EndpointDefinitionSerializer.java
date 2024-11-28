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

package org.apache.synapse.config.xml.endpoints;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.statistics.StatisticsConfigurable;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.endpoints.EndpointDefinition;

public class EndpointDefinitionSerializer {
    private OMFactory fac = OMAbstractFactory.getOMFactory();

    public void serializeEndpointDefinition(EndpointDefinition endpointDefinition,
                                                 OMElement element) {

        StatisticsConfigurable statisticsConfigurable =
                endpointDefinition.getAspectConfiguration();

        if (statisticsConfigurable != null &&
                statisticsConfigurable.isStatisticsEnable()) {

            element.addAttribute(fac.createOMAttribute(
                    XMLConfigConstants.STATISTICS_ATTRIB_NAME, null,
                    XMLConfigConstants.STATISTICS_ENABLE));
        }

        if (statisticsConfigurable != null &&
            statisticsConfigurable.isTracingEnabled()) {

            element.addAttribute(fac.createOMAttribute(
                    XMLConfigConstants.TRACE_ATTRIB_NAME, null,
                    XMLConfigConstants.TRACE_ENABLE));
        }

        if (endpointDefinition.isUseSwa()) {
            element.addAttribute(fac.createOMAttribute("optimize", null, "swa"));
        } else if (endpointDefinition.isUseMTOM()) {
            element.addAttribute(fac.createOMAttribute("optimize", null, "mtom"));
        }

        if (endpointDefinition.getCharSetEncoding() != null) {
            element.addAttribute(fac.createOMAttribute(
                    "encoding", null, endpointDefinition.getCharSetEncoding()));
        }

        if (endpointDefinition.isAddressingOn()) {
            OMElement addressing = fac.createOMElement(
                    "enableAddressing", SynapseConstants.SYNAPSE_OMNAMESPACE);

            if (endpointDefinition.getAddressingVersion() != null) {
                addressing.addAttribute(fac.createOMAttribute(
                        "version", null, endpointDefinition.getAddressingVersion()));
            }

            if (endpointDefinition.isUseSeparateListener()) {
                addressing.addAttribute(fac.createOMAttribute("separateListener", null, "true"));
            }
            element.addChild(addressing);
        }

        if (endpointDefinition.isSecurityOn()) {
            OMElement sec = fac.createOMElement("enableSec", SynapseConstants.SYNAPSE_OMNAMESPACE);

            if (endpointDefinition.getWsSecPolicyKey() != null) {
                if (!endpointDefinition.isDynamicPolicy()) {
                    sec.addAttribute(fac.createOMAttribute(
                            "policy", null, endpointDefinition.getWsSecPolicyKey()));
                } else {
                    sec.addAttribute(fac.createOMAttribute(
                            "policy", null, '{' + endpointDefinition.getDynamicPolicy().getExpression() + '}'));
                }
            } else {
                if (endpointDefinition.getInboundWsSecPolicyKey() != null) {
                    sec.addAttribute(fac.createOMAttribute(
                            "inboundPolicy", null, endpointDefinition.getInboundWsSecPolicyKey()));
                }
                if (endpointDefinition.getOutboundWsSecPolicyKey() != null) {
                    sec.addAttribute(fac.createOMAttribute("outboundPolicy",
                            null, endpointDefinition.getOutboundWsSecPolicyKey()));
                }
            }
            element.addChild(sec);
        }

        if (!endpointDefinition.getTimeoutAction().equals("none") || endpointDefinition.isTimeoutActionDynamic() ||
                isStringPositiveNumber(endpointDefinition.getTimeoutDuration())  || endpointDefinition.isDynamicTimeoutEndpoint()) {

            OMElement timeout = fac.createOMElement(
                    "timeout", SynapseConstants.SYNAPSE_OMNAMESPACE);
            element.addChild(timeout);

            if (isStringPositiveNumber(endpointDefinition.getTimeoutDuration()) || endpointDefinition.isDynamicTimeoutEndpoint()) {
                OMElement duration = fac.createOMElement(
                        "duration", SynapseConstants.SYNAPSE_OMNAMESPACE);
                if (!endpointDefinition.isDynamicTimeoutEndpoint()) {
                    duration.setText(endpointDefinition.getTimeoutDuration());
                } else {
                    duration.setText('{' + endpointDefinition.getTimeoutDuration() + '}');
                }
                timeout.addChild(duration);
            }

            if (!endpointDefinition.getTimeoutAction().equals("none") || endpointDefinition.isTimeoutActionDynamic()) {
                OMElement action = fac.createOMElement("responseAction", SynapseConstants.SYNAPSE_OMNAMESPACE);
                if (endpointDefinition.isTimeoutActionDynamic()) {
                    action.setText('{' + endpointDefinition.getTimeoutAction() + '}');
                } else {
                    if (endpointDefinition.getTimeoutAction().equals("discard")) {
                        action.setText("discard");
                    } else if (endpointDefinition.getTimeoutAction().equals("fault")) {
                        action.setText("fault");
                    }
                }
                timeout.addChild(action);
            }
        }

        if (!endpointDefinition.getInitialSuspendDuration().equals(String.valueOf(-1)) || endpointDefinition.isInitialSuspendDurationDynamic() ||
            !endpointDefinition.getSuspendErrorCodes().isEmpty() || endpointDefinition.isSuspendErrorCodesDynamic()) {

            OMElement suspendOnFailure = fac.createOMElement(
                org.apache.synapse.config.xml.XMLConfigConstants.SUSPEND_ON_FAILURE,
                SynapseConstants.SYNAPSE_OMNAMESPACE);

            if (!endpointDefinition.getSuspendErrorCodes().isEmpty() || endpointDefinition.isSuspendErrorCodesDynamic()) {
                OMElement errorCodes = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.ERROR_CODES,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                if (endpointDefinition.isSuspendErrorCodesDynamic()) {
                    errorCodes.setText('{' + endpointDefinition.getSuspendErrorCodes() + '}');
                } else {
                    errorCodes.setText(endpointDefinition.getSuspendErrorCodes().
                            toString().replaceAll("[\\[\\] ]", ""));
                }
                suspendOnFailure.addChild(errorCodes);
            }

            if (!endpointDefinition.getInitialSuspendDuration().equals(String.valueOf(-1)) || endpointDefinition.isInitialSuspendDurationDynamic()) {
                OMElement initialDuration = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.SUSPEND_INITIAL_DURATION,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                if (!endpointDefinition.isInitialSuspendDurationDynamic()){
                    initialDuration.setText(endpointDefinition.getInitialSuspendDuration());
                } else {
                    initialDuration.setText('{' + endpointDefinition.getInitialSuspendDuration() + '}');
                }
                suspendOnFailure.addChild(initialDuration);
            }

            if (!endpointDefinition.getSuspendProgressionFactor().equals(String.valueOf(-1)) || endpointDefinition.isSuspendProgressionFactorDynamic()) {
                OMElement progressionFactor = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.SUSPEND_PROGRESSION_FACTOR,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                if (endpointDefinition.isSuspendProgressionFactorDynamic()) {
                    progressionFactor.setText('{' + endpointDefinition.getSuspendProgressionFactor() + '}');
                } else {
                    progressionFactor.setText(endpointDefinition.getSuspendProgressionFactor());
                }
                suspendOnFailure.addChild(progressionFactor);
            }

            if ((!endpointDefinition.getSuspendMaximumDuration().equals(String.valueOf(-1)) &&
                    !endpointDefinition.getSuspendMaximumDuration().equals(String.valueOf(Long.MAX_VALUE))) || endpointDefinition.isSuspendMaximumDurationDynamic()) {
                OMElement suspendMaximum = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.SUSPEND_MAXIMUM_DURATION,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                if (endpointDefinition.isSuspendMaximumDurationDynamic()) {
                    suspendMaximum.setText('{' + endpointDefinition.getSuspendMaximumDuration() + '}');
                } else {
                    suspendMaximum.setText(endpointDefinition.getSuspendMaximumDuration());
                }
                suspendOnFailure.addChild(suspendMaximum);
            }

            element.addChild(suspendOnFailure);
        }

        if (isStringPositiveNumber(endpointDefinition.getRetryDurationOnTimeout()) || endpointDefinition.isRetryDurationOnTimeoutDynamic() ||
            !endpointDefinition.getTimeoutErrorCodes().isEmpty() || endpointDefinition.isTimeoutErrorCodesDynamic()) {

            OMElement markAsTimedout = fac.createOMElement(
                org.apache.synapse.config.xml.XMLConfigConstants.MARK_FOR_SUSPENSION,
                SynapseConstants.SYNAPSE_OMNAMESPACE);

            if (!endpointDefinition.getTimeoutErrorCodes().isEmpty() || endpointDefinition.isTimeoutErrorCodesDynamic()) {
                OMElement errorCodes = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.ERROR_CODES,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                if (endpointDefinition.isTimeoutErrorCodesDynamic()) {
                    errorCodes.setText('{' + endpointDefinition.getTimeoutErrorCodes() + '}');
                } else {
                    errorCodes.setText(endpointDefinition.getTimeoutErrorCodes().
                            toString().replaceAll("[\\[\\] ]", ""));
                }
                markAsTimedout.addChild(errorCodes);
            }

            if (isStringPositiveNumber(endpointDefinition.getRetriesOnTimeoutBeforeSuspend()) || endpointDefinition.isRetriesOnTimeoutBeforeSuspendDynamic()) {
                OMElement retries = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.RETRIES_BEFORE_SUSPENSION,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                if (endpointDefinition.isRetriesOnTimeoutBeforeSuspendDynamic()) {
                    retries.setText('{' + endpointDefinition.getRetriesOnTimeoutBeforeSuspend() + '}');
                } else {
                    retries.setText(endpointDefinition.getRetriesOnTimeoutBeforeSuspend());
                }
                markAsTimedout.addChild(retries);
            }

            if (isStringPositiveNumber(endpointDefinition.getRetryDurationOnTimeout()) || endpointDefinition.isRetryDurationOnTimeoutDynamic()) {
                OMElement retryDelay = fac.createOMElement(
                    org.apache.synapse.config.xml.XMLConfigConstants.RETRY_DELAY,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
                if (endpointDefinition.isRetryDurationOnTimeoutDynamic()) {
                    retryDelay.setText('{' + endpointDefinition.getRetryDurationOnTimeout() + '}');
                } else {
                    retryDelay.setText(endpointDefinition.getRetryDurationOnTimeout());
                }
                markAsTimedout.addChild(retryDelay);
            }

            element.addChild(markAsTimedout);
        }

        if (!endpointDefinition.getRetryDisabledErrorCodes().isEmpty()) {
            OMElement retryConfig = fac.createOMElement(XMLConfigConstants.RETRY_CONFIG,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
            OMElement errorCodes = fac.createOMElement("disabledErrorCodes",
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
            errorCodes.setText(endpointDefinition.getRetryDisabledErrorCodes().
                    toString().replaceAll("[\\[\\] ]", ""));
            retryConfig.addChild(errorCodes);
            element.addChild(retryConfig);
        } else if (!endpointDefinition.getRetryEnableErrorCodes().isEmpty()) {
            OMElement retryConfig = fac.createOMElement(XMLConfigConstants.RETRY_CONFIG,
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
            OMElement errorCodes = fac.createOMElement("enabledErrorCodes",
                    SynapseConstants.SYNAPSE_OMNAMESPACE);
            errorCodes.setText(endpointDefinition.getRetryEnableErrorCodes().
                    toString().replaceAll("[\\[\\] ]", ""));
            retryConfig.addChild(errorCodes);
            element.addChild(retryConfig);
        }
    }

    private boolean isStringPositiveNumber(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        try {
            float number = Float.parseFloat(input);
            return number > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
