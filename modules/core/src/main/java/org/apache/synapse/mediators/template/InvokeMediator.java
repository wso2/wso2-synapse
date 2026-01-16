/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.mediators.template;

import com.google.gson.JsonObject;
import org.apache.axis2.transport.TransportUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.continuation.ReliantContinuationState;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.data.connector.ConnectorResponse;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.FlowContinuableMediator;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.mediators.elementary.EnrichMediator;
import org.apache.synapse.mediators.elementary.Source;
import org.apache.synapse.mediators.elementary.Target;
import org.apache.synapse.transport.customlogsetter.CustomLogSetter;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.util.MediatorEnrichUtil;
import org.apache.synapse.util.synapse.expression.constants.ExpressionConstants;

import java.util.*;

import static org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS;
import static org.apache.synapse.mediators.builtin.CallMediator.ORIGINAL_CONTENT_TYPE;
import static org.apache.synapse.mediators.builtin.CallMediator.ORIGINAL_MESSAGE_TYPE;
import static org.apache.synapse.mediators.builtin.CallMediator.ORIGINAL_NO_ENTITY_BODY;
import static org.apache.synapse.mediators.builtin.CallMediator.ORIGINAL_TRANSPORT_HEADERS;

/**
 * This class handles invocation of a synapse function template. <invoke
 * target=""> <parameter name="p1" value="{expr} | {{expr}} | value" />* ..
 * </invoke>
 */
public class InvokeMediator extends AbstractMediator implements
                                                     ManagedLifecycle,FlowContinuableMediator {

	/**
	 * Unique identifier for the invoke mediator
	 */
	private String id;

	/**
	 * refers to the target template this is going to invoke this is a read only
	 * attribute of the mediator
	 */
	private String targetTemplate;

	/** The name of the error handler which is used to
	 * handle error during the mediation
	 */
	private String errorHandler = null;

	/**
	 * Refers to the parent package qualified reference
	 * 
	 */
	private String packageName;

	/**
	 * maps each parameter name to a {@link InvokeParam} this is a read only
	 * attribute of the mediator
	 */
	private Map<String, InvokeParam> pName2ParamMap;

    /**
     * Flag to identify whether this InvokeMediator is used to invoke a connector/module operation
     */
	private boolean dynamicMediator = false;
	
	private Value key = null;

	private String localEntryKey = null;

	/**
	 * Reference to the synapse environment
	 */
	private SynapseEnvironment synapseEnv;

	private static final Random RANDOM = new Random();

    //

    /**
     * Flag to identify whether this InvokeMediator is used to invoke a connector operation
     * This will help to differentiate between connectors and modules in open-telemetry tracing
     */
    private boolean isConnector = false;

	public InvokeMediator() {
		// LinkedHashMap is used to preserve tag order
		pName2ParamMap = new LinkedHashMap<>();
		id = String.valueOf(RANDOM.nextLong());
	}

    public boolean mediate(MessageContext synCtx) {
        return mediate(synCtx, true);
    }

	private boolean mediate(MessageContext synCtx, boolean executePreFetchingSequence) {

		if (synCtx.getEnvironment().isDebuggerEnabled()) {
			if (super.divertMediationRoute(synCtx)) {
				return true;
			}
		}

		SynapseLog synLog = getLog(synCtx);

		if (synLog.isTraceOrDebugEnabled()) {
			synLog.traceOrDebug("Invoking Target EIP Sequence " + targetTemplate +
			                    " paramNames : " + pName2ParamMap.keySet());
			if (synLog.isTraceTraceEnabled()) {
				synLog.traceTrace("Message : " + synCtx.getEnvelope());
			}
		}
		// get the target function template and invoke by passing populated
		// parameters
		Mediator mediator = synCtx.getSequenceTemplate(targetTemplate);

		if (mediator == null) {
			handleException("Sequence template " +
					targetTemplate + " cannot be found", synCtx);
		}

        //setting the log appender when external template executor is called a sequence template inside a car file
        if (mediator instanceof TemplateMediator) {
            CustomLogSetter.getInstance().setLogAppender(((TemplateMediator) mediator)
                                                                 .getArtifactContainerName());
        }

		// executing key reference if found defined at configuration.
		if (executePreFetchingSequence && key != null) {
			String defaultConfiguration = key.evaluateValue(synCtx);
			Mediator m = synCtx.getDefaultConfiguration(defaultConfiguration);
			if (m instanceof InvokeMediator) {
				InvokeMediator invokeMediator = (InvokeMediator) m;
				invokeMediator.setLocalEntryKey(defaultConfiguration);
			}
			if (m == null) {
				handleException("Sequence named " + key + " cannot be found", synCtx);

			} else {
				if (synLog.isTraceOrDebugEnabled()) {
					synLog.traceOrDebug("Executing with key " + key);
				}
                ContinuationStackManager.addReliantContinuationState(
                        synCtx, 1, getMediatorPosition());
				boolean result = m.mediate(synCtx);

                if (result) {
                    ContinuationStackManager.removeReliantContinuationState(synCtx);
                } else {
                    return false;
                }
            }
		}

		if (mediator != null && mediator instanceof TemplateMediator) {
			if (localEntryKey != null) {
				((TemplateMediator) mediator).setLocalEntryKey(localEntryKey);
			}
			populateParameters(synCtx, ((TemplateMediator) mediator).getName());
			if (executePreFetchingSequence) {
				ContinuationStackManager.addReliantContinuationState(synCtx,
						0, getMediatorPosition());
			}
			Mediator errorHandlerMediator = null;
			if (errorHandler != null) {
				errorHandlerMediator = synCtx.getSequence(errorHandler);
				if (errorHandlerMediator != null) {
					if (synLog.isTraceOrDebugEnabled()) {
						synLog.traceOrDebug("Setting the onError handler : "
								+ errorHandler + " when invoking : "
								+ targetTemplate);
					}
					synCtx.pushFaultHandler(new MediatorFaultHandler(errorHandlerMediator));
				}
			}

			prepareForMediation(synCtx);
			boolean result = mediator.mediate(synCtx);

			if (result && executePreFetchingSequence) {
				ContinuationStackManager.removeReliantContinuationState(synCtx);
				postMediate(synCtx);
			}

			if (errorHandlerMediator != null) {
				Stack faultStack = synCtx.getFaultStack();
				if (faultStack != null && !faultStack.isEmpty()) {
					Object o = faultStack.peek();
					if (o instanceof MediatorFaultHandler
							&& errorHandlerMediator.equals(((MediatorFaultHandler) o).getFaultMediator())) {
						faultStack.pop();
					}
				}
			}
			return result;
		}
		return false;
	}

	@Override
	public boolean isContentAware() {
		//evaluate parameters with expression
		for (Map.Entry<String, InvokeParam> parameterEntry : pName2ParamMap.entrySet()) {
			String parameterName = parameterEntry.getKey();
			if (!"".equals(parameterName)) {
				if (SynapseConstants.RESPONSE_VARIABLE.equals(parameterName) ||
						SynapseConstants.OVERWRITE_BODY.equals(parameterName)) {
					return true;
				}
				InvokeParam parameter = parameterEntry.getValue();
				if (isContentAware(parameter)) {
					return true;
				}
			}
		}
		return false;
	}

    private boolean isContentAware(InvokeParam param) {

        if (param == null) {
            return false;
        }

        // Check whether the inline value is content aware
        if (param.getInlineValue() != null) {
            SynapsePath inlineValueExpression = param.getInlineValue().getExpression();
            if (inlineValueExpression != null && inlineValueExpression.isContentAware()) {
                return true;
            }
        }

        // Check whether the attribute values are content aware
        if (param.getAttributeName2ExpressionMap() != null) {
            for (String attributeName : param.getAttributeName2ExpressionMap().keySet()) {
                if (!"".equals(attributeName)) {
                    Value attributeValue = param.getAttributeName2ExpressionMap().get(attributeName);
                    if (attributeValue != null && attributeValue.getExpression() != null &&
                            attributeValue.getExpression().isContentAware()) {
                        return true;
                    }
                }
            }
        }

        // Check whether the child parameters are content aware
        if (param.getChildParams() != null) {
            for (InvokeParam childParam : param.getChildParams()) {
                if (isContentAware(childParam)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mediate(MessageContext synCtx, ContinuationState continuationState) {
        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Invoke mediator : Mediating from ContinuationState");
        }

        boolean result;
        int subBranch = ((ReliantContinuationState) continuationState).getSubBranch();
		boolean isStatisticsEnabled = RuntimeStatisticCollector.isStatisticsEnabled();
        if (subBranch == 0) {
	        // Default flow
	        TemplateMediator templateMediator = (TemplateMediator) synCtx.getSequenceTemplate(targetTemplate);
	        if (!continuationState.hasChild()) {
		        result = templateMediator.mediate(synCtx, continuationState.getPosition() + 1);
				postMediate(synCtx);
		        if (result) {
			        templateMediator.popFuncContextFrom(synCtx);
		        }
	        } else {
		        FlowContinuableMediator mediator =
				        (FlowContinuableMediator) templateMediator.getChild(continuationState.getPosition());

		        result = mediator.mediate(synCtx, continuationState.getChildContState());

				if (isStatisticsEnabled) {
					((Mediator) mediator).reportCloseStatistics(synCtx, null);
				}
	        }
			if (isStatisticsEnabled) {
				templateMediator.reportCloseStatistics(synCtx, null);
			}
        } else {
	        // Pre fetching invoke mediator flow
	        String prefetchInvokeKey = key.evaluateValue(synCtx);
	        InvokeMediator prefetchInvoke = (InvokeMediator) synCtx.getDefaultConfiguration(prefetchInvokeKey);

	        ContinuationState childContinuationState = continuationState.getChildContState();
	        result = prefetchInvoke.mediate(synCtx, childContinuationState);
			postMediate(synCtx);

	        if (result && !childContinuationState.hasChild()) {
		        // Pre fetching invoke mediator flow completed.
		        // Remove ContinuationState represent the prefetchInvoke mediator and
		        // flip the subbranch to default flow
		        continuationState.removeLeafChild();
		        ((ReliantContinuationState) continuationState).setSubBranch(0);
		        // after prefetch invoke mediator flow, execute default flow
		        result = mediate(synCtx, false);
	        }
			if (isStatisticsEnabled) {
				prefetchInvoke.reportCloseStatistics(synCtx, null);
			}
        }
        return result;
    }

	/**
	 * poplulate declared parameters on temp synapse properties
	 * 
	 * @param synCtx
	 * @param templateQualifiedName
	 */
	private void populateParameters(MessageContext synCtx, String templateQualifiedName) {

		EIPUtils.createSynapseEIPTemplateProperty(synCtx, templateQualifiedName, SynapseConstants.INVOKE_MEDIATOR_ID, id);
		for (Map.Entry<String, InvokeParam> paramEntry : pName2ParamMap.entrySet()) {
			String parameter = paramEntry.getKey();
			if (!"".equals(parameter)) {
				InvokeParam connectorParam = paramEntry.getValue();
				if (hasChildOrAttributeParam(connectorParam)) {
					EIPUtils.createSynapseEIPTemplateProperty(synCtx, templateQualifiedName, parameter, connectorParam);
				} else {
					// For backward compatibility, if there are no nested parameters, store the inline expression value
					// directly in the Synapse context instead of InvokeParam object.
					Value expression = connectorParam.getInlineValue();
					if (expression != null) {
						EIPUtils.createSynapseEIPTemplateProperty(synCtx, templateQualifiedName, parameter, expression);
					}
				}
			}
		}
	}

    /**
     * Check whether the invoke parameter has child parameters or attribute parameters
     *
     * @param invokeParam invoke parameter
     * @return true if the invoke parameter has child parameters or attribute parameters
     */
    private boolean hasChildOrAttributeParam(InvokeParam invokeParam) {

        return (invokeParam.getChildParams() != null && !invokeParam.getChildParams().isEmpty()) ||
                (invokeParam.getAttributeName2ExpressionMap() != null &&
                        !invokeParam.getAttributeName2ExpressionMap().isEmpty());
    }

    private boolean storeResponseInVariableEnabled(MessageContext synCtx) {

        if (pName2ParamMap.containsKey(SynapseConstants.RESPONSE_VARIABLE)) {
            Value responseVariable = pName2ParamMap.get(SynapseConstants.RESPONSE_VARIABLE).getInlineValue();
            if (responseVariable != null) {
                String responseVariableValue = responseVariable.evaluateValue(synCtx);
                if (log.isDebugEnabled()) {
                    log.debug("Response variable value: " + responseVariableValue);
                }
                if (StringUtils.isNotEmpty(responseVariableValue)) {
                    return true;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Response variable value or overwrite body value is null");
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Response does not needs to be stored in a variable");
        }
        return false;
    }

    private void prepareForMediation(MessageContext synCtx) {

        if (!storeResponseInVariableEnabled(synCtx)) {
            return;
        }
        boolean overwriteBody = isOverwriteBodyEnabled(synCtx);
        if (!overwriteBody) {
            org.apache.axis2.context.MessageContext axis2MessageContext =
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            Object messageType = axis2MessageContext.getProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE);
            Object contentType = axis2MessageContext.getProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE);
            Object headers = axis2MessageContext.getProperty(TRANSPORT_HEADERS);
            Map transportHeadersMap = (Map) headers;
            // Create a clone of the original transport headers
            transportHeadersMap = new TreeMap(transportHeadersMap);
            synCtx.setProperty(ORIGINAL_MESSAGE_TYPE + "_" + SynapseConstants.BEFORE_INVOKE_TEMPLATE, messageType);
            synCtx.setProperty(ORIGINAL_CONTENT_TYPE + "_" + SynapseConstants.BEFORE_INVOKE_TEMPLATE, contentType);
            synCtx.setProperty(ORIGINAL_TRANSPORT_HEADERS + "_" +
                    SynapseConstants.BEFORE_INVOKE_TEMPLATE, transportHeadersMap);
            Object noEntityBody = axis2MessageContext.getProperty(PassThroughConstants.NO_ENTITY_BODY);
            synCtx.setProperty(ORIGINAL_NO_ENTITY_BODY + "_" +
                    SynapseConstants.BEFORE_INVOKE_TEMPLATE, noEntityBody);
            if (!Boolean.TRUE.equals(noEntityBody)) {
                Source source = MediatorEnrichUtil.createSourceWithBody();
                String targetPropertyName = SynapseConstants.ORIGINAL_PAYLOAD + "_" +
                        SynapseConstants.BEFORE_INVOKE_TEMPLATE;
                Target target = MediatorEnrichUtil.createTargetWithProperty(targetPropertyName);
                doEnrich(synCtx, source, target);
            }
        }
    }

    private boolean isOverwriteBodyEnabled(MessageContext synCtx) {

        InvokeParam overwriteBodyParam = pName2ParamMap.get(SynapseConstants.OVERWRITE_BODY);
        boolean overwriteBody = false;
        if (overwriteBodyParam != null && overwriteBodyParam.getInlineValue() != null) {
            overwriteBody = Boolean.parseBoolean(overwriteBodyParam.getInlineValue().evaluateValue(synCtx));
        }
        return overwriteBody;
    }

    private void postMediate(MessageContext synCtx) {

        if (!storeResponseInVariableEnabled(synCtx)) {
            return;
        }
        boolean overwriteBody = isOverwriteBodyEnabled(synCtx);
        processConnectorResponse(synCtx, overwriteBody);
        if (!overwriteBody) {
            Map originalTransportHeaders =
                    (Map) synCtx.getProperty(ORIGINAL_TRANSPORT_HEADERS + "_" +
                            SynapseConstants.BEFORE_INVOKE_TEMPLATE);
            org.apache.axis2.context.MessageContext axis2MsgCtx =
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            axis2MsgCtx.setProperty(TRANSPORT_HEADERS, originalTransportHeaders);
            axis2MsgCtx.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE,
                    synCtx.getProperty(ORIGINAL_MESSAGE_TYPE + "_" + SynapseConstants.BEFORE_INVOKE_TEMPLATE));
            axis2MsgCtx.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE,
                    synCtx.getProperty(ORIGINAL_CONTENT_TYPE + "_" + SynapseConstants.BEFORE_INVOKE_TEMPLATE));
            Object noEntityBody = synCtx.getProperty(ORIGINAL_NO_ENTITY_BODY + "_" +
                    SynapseConstants.BEFORE_INVOKE_TEMPLATE);
            if (!Boolean.TRUE.equals(noEntityBody)) {
                Source sourceForResponsePayload = MediatorEnrichUtil.createSourceWithProperty(
                        SynapseConstants.ORIGINAL_PAYLOAD + "_" + SynapseConstants.BEFORE_INVOKE_TEMPLATE);
                Target targetForResponsePayload = MediatorEnrichUtil.createTargetWithBody();
                doEnrich(synCtx, sourceForResponsePayload, targetForResponsePayload);
            } else {
                axis2MsgCtx.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
                TransportUtils.createSOAPEnvelope(null);
            }

            // Remove the temporary properties
            synCtx.getPropertyKeySet().remove(SynapseConstants.ORIGINAL_PAYLOAD + "_" +
                    SynapseConstants.BEFORE_INVOKE_TEMPLATE);
            synCtx.getPropertyKeySet().remove(ORIGINAL_MESSAGE_TYPE + "_" + SynapseConstants.BEFORE_INVOKE_TEMPLATE);
            synCtx.getPropertyKeySet().remove(ORIGINAL_CONTENT_TYPE + "_" + SynapseConstants.BEFORE_INVOKE_TEMPLATE);
            synCtx.getPropertyKeySet().remove(ORIGINAL_NO_ENTITY_BODY + "_" +
                    SynapseConstants.BEFORE_INVOKE_TEMPLATE);
            synCtx.getPropertyKeySet().remove(ORIGINAL_TRANSPORT_HEADERS + "_" +
                    SynapseConstants.BEFORE_INVOKE_TEMPLATE);
        }

    }

	public String getTargetTemplate() {
		return targetTemplate;
	}

	public void setTargetTemplate(String targetTemplate) {
		this.targetTemplate = targetTemplate;
	}

	public void setErrorHandler(String errorHandler) {
		this.errorHandler = errorHandler;
	}

	public String getErrorHandler() {
		return errorHandler;
	}

    public Map<String, InvokeParam> getpName2ParamMap() {

        return pName2ParamMap;
    }

    public void addInvokeParam(String pName, InvokeParam param) {

        pName2ParamMap.put(pName, param);
    }

	public boolean isDynamicMediator() {
		return dynamicMediator;
	}

	public void setDynamicMediator(boolean dynamicMediator) {
		this.dynamicMediator = dynamicMediator;
	}

	public Value getKey() {
		return key;
	}

	public void setKey(Value key) {
		this.key = key;
	}
	public void setLocalEntryKey(String localEntryKey) {
		this.localEntryKey = localEntryKey;
	}
	public String getLocalEntryKey() {
		return localEntryKey;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

    public void init(SynapseEnvironment se) {
        synapseEnv = se;

        TemplateMediator templateMediator =
                se.getSynapseConfiguration().getSequenceTemplate(targetTemplate);
        // TemplateMediator is initializing only if it's not in the initializing status to overcome  
        // recursive initialisation issue when a template is used in a recursive manner
        if (templateMediator != null && !templateMediator.isInitializing()) {
            templateMediator.init(se);
        }
        if (templateMediator == null || templateMediator.isDynamic()) {
            // undefined or dynamic templates are treated as unavailable
            // in the environment.
            // At the time of their initialization, these will be marked as available.
            se.addUnavailableArtifactRef(targetTemplate);
        }
    }

    private void processConnectorResponse(MessageContext synCtx, boolean overwriteBody) {

        Value responseVariable = pName2ParamMap.get(SynapseConstants.RESPONSE_VARIABLE).getInlineValue();
        String responseVariableName = responseVariable.evaluateValue(synCtx);
        ConnectorResponse connectorResponse = (ConnectorResponse) synCtx.getVariable(responseVariableName);
        Map<String, Object> responseMap = new HashMap<>();
        if (connectorResponse == null) {
            Source sourceForResponsePayload = MediatorEnrichUtil.createSourceWithBody();
            Target targetForResponsePayload = new Target();
            targetForResponsePayload.setTargetType(EnrichMediator.VARIABLE);
            targetForResponsePayload.setVariable(responseVariable);
            doEnrich(synCtx, sourceForResponsePayload, targetForResponsePayload);
            if (overwriteBody) {
                // If overwrite body is enabled, no need to store the payload in the variable
                Map response = (Map) synCtx.getVariable(responseVariableName);
                response.remove(ExpressionConstants.PAYLOAD);
            }
        } else {
            Object payload = connectorResponse.getPayload();
            if (!overwriteBody && payload != null) {
                responseMap.put(ExpressionConstants.PAYLOAD, payload);
            }
            Map<String, Object> headers = connectorResponse.getHeaders();
            if (!headers.isEmpty()) {
                responseMap.put(ExpressionConstants.HEADERS, convertMapToJson(headers));
            }
            Map<String, Object> attributes = connectorResponse.getAttributes();
            if (!attributes.isEmpty()) {
                responseMap.put(ExpressionConstants.ATTRIBUTES, convertMapToJson(attributes));
            }
            synCtx.setVariable(responseVariableName, responseMap);
        }
    }

    private void doEnrich(MessageContext synCtx, Source source, Target target) {

        // If the message builder is not invoked, build the message
        if (!Boolean.TRUE.equals(synCtx.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED))) {
            MediatorEnrichUtil.buildMessage(synCtx);
        }
        EnrichMediator enrichMediator = new EnrichMediator();
        enrichMediator.setSource(source);
        enrichMediator.setTarget(target);
        enrichMediator.setNativeJsonSupportEnabled(true);
        enrichMediator.mediate(synCtx);
    }

    private JsonObject convertMapToJson(Map<String, Object> map) {

        JsonObject jsonObject = new JsonObject();
        map.forEach((key, value) -> {
            if (value instanceof Number) {
                jsonObject.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                jsonObject.addProperty(key, (Boolean) value);
            } else {
                jsonObject.addProperty(key, value.toString());
            }
        });
        return jsonObject;
    }

    public void destroy() {
        TemplateMediator templateMediator =
                synapseEnv.getSynapseConfiguration().getSequenceTemplate(targetTemplate);
        if (templateMediator == null || templateMediator.isDynamic()) {
            synapseEnv.removeUnavailableArtifactRef(targetTemplate);
        }
    }

	@Override
	public void setComponentStatisticsId(ArtifactHolder holder) {
		if (getAspectConfiguration() == null) {
			configure(new AspectConfiguration(getMediatorName()));
		}
		String mediatorId =
				StatisticIdentityGenerator.getIdForFlowContinuableMediator(getMediatorName(), ComponentType.MEDIATOR, holder);
		getAspectConfiguration().setUniqueId(mediatorId);
		StatisticIdentityGenerator.reportingFlowContinuableEndEvent(mediatorId, ComponentType.MEDIATOR, holder);
	}

    public boolean isConnector() {
        return isConnector;
    }

    public void setConnector(boolean connector) {
        isConnector = connector;
    }

    private String getInvokingArtifactName() {
        if (!isDynamicMediator()) {
            return super.getMediatorName();
        }

        String operation = this.getTargetTemplate().replaceAll(".*\\.([^.]+\\.[^.]+)$", "$1");
        return super.getMediatorName() + ":" + (isConnector() ? "Connector" : "Module") + "[" + operation + "]";
    }

    @Override
    public Integer reportOpenStatistics(MessageContext messageContext, boolean isContentAltering) {
        Integer index = OpenEventCollector
                .reportFlowContinuableEvent(messageContext, getInvokingArtifactName(), ComponentType.MEDIATOR,
                        getAspectConfiguration(), isContentAltering() || isContentAltering);

        if (isDynamicMediator()) {
            messageContext.setProperty(StatisticsConstants.ATOMIC_UNIT_ACTIVE, true);
        }

        return index;
    }

    @Override
    public void reportCloseStatistics(MessageContext messageContext, Integer currentIndex) {
        CloseEventCollector.closeEntryEvent(messageContext, getInvokingArtifactName(), ComponentType.MEDIATOR,
                currentIndex, isContentAltering());

        if (isDynamicMediator()) {
            messageContext.setProperty(StatisticsConstants.ATOMIC_UNIT_ACTIVE, false);
        }
    }
}
