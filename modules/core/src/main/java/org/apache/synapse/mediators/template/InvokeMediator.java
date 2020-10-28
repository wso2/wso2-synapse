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

import org.apache.synapse.ContinuationState;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.continuation.ReliantContinuationState;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.FlowContinuableMediator;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.transport.customlogsetter.CustomLogSetter;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

/**
 * This class handles invocation of a synapse function template. <invoke
 * target=""> <parameter name="p1" value="{expr} | {{expr}} | value" />* ..
 * </invoke>
 */
public class InvokeMediator extends AbstractMediator implements
                                                     ManagedLifecycle,FlowContinuableMediator {
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
	 * maps each parameter name to a Expression/Value this is a read only
	 * attribute of the mediator
	 */
	private Map<String, Value> pName2ExpressionMap;

	private boolean dynamicMediator = false;

	/** The local registry key which is used to pick a sequence definition */
	private Value key = null;

    /** Reference to the synapse environment */
    private SynapseEnvironment synapseEnv;

	public InvokeMediator() {
		// LinkedHashMap is used to preserve tag order
		pName2ExpressionMap = new LinkedHashMap<String, Value>();
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
			                    " paramNames : " + pName2ExpressionMap.keySet());
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

			boolean result = mediator.mediate(synCtx);

			if (result && executePreFetchingSequence) {
				ContinuationStackManager.removeReliantContinuationState(synCtx);
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
		Iterator<String> parameterNames = pName2ExpressionMap.keySet().iterator();
		while (parameterNames.hasNext()) {
			String parameterName = parameterNames.next();
			if (!"".equals(parameterName)) {
				Value parameter = pName2ExpressionMap.get(parameterName);
				SynapsePath expression = parameter.getExpression();
				if (expression != null && expression.isContentAware()) {
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
		Iterator<String> params = pName2ExpressionMap.keySet().iterator();
		while (params.hasNext()) {
			String parameter = params.next();
			if (!"".equals(parameter)) {
				Value expression = pName2ExpressionMap.get(parameter);
				if (expression != null) {
					EIPUtils.createSynapseEIPTemplateProperty(synCtx, templateQualifiedName,
					                                          parameter, expression);
				}
			}
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

	public Map<String, Value> getpName2ExpressionMap() {
		return pName2ExpressionMap;
	}

	public void addExpressionForParamName(String pName, Value expr) {
		pName2ExpressionMap.put(pName, expr);
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
        if (templateMediator == null || templateMediator.isDynamic()) {
            // undefined or dynamic templates are treated as unavailable
            // in the environment.
            // At the time of their initialization, these will be marked as available.
            se.addUnavailableArtifactRef(targetTemplate);
        }
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
}
