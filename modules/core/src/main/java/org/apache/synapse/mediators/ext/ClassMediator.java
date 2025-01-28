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

package org.apache.synapse.mediators.ext;

import org.apache.axis2.AxisFault;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.config.xml.PropertyHelper;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.mediators.v2.Utils;
import org.apache.synapse.mediators.v2.ext.AbstractClassMediator;
import org.apache.synapse.mediators.v2.ext.InputArgument;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The class mediator delegates the mediation to a single instance of a specified
 * class. The specified class must implement the Mediator interface and optionally
 * may implement the ManagedLifecycle interface. At initialization time, a single
 * instance of the class is instantiated using a public no argument constructor, and
 * any one-time properties (parameter constants specified through the Synapse config)
 * are set on the instance. If each request needs synchronization, the user must
 * implement it within the specified class.
 * 
 * @see Mediator
 */
public class ClassMediator extends AbstractMediator implements ManagedLifecycle {

    /** The reference to the actual class that implments the Mediator interface */
    private Mediator mediator = null;
    /** The holder for the custom properties */
    private final List<MediatorProperty> properties = new ArrayList<MediatorProperty>();

    /** Contains Dynamic Expressions/not */
    private boolean hasDynamicProperties = false;

    private List<AbstractClassMediator.Arg> arguments = new ArrayList<>();
    private Map<String, InputArgument> inputArguments;
    private String methodName = "mediate";
    private String resultTarget;
    private String variableName;

    /**
	 * Don't use a new instance... do one instance of the object per instance of
	 * this mediator
	 * 
	 * @param synCtx
	 *            the message context
	 * @return as per standard semantics
	 */
	public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Class mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        if (synLog.isTraceOrDebugEnabled()) {
			synLog.traceOrDebug("invoking : " + mediator.getClass() + ".mediate()");
		}

        boolean result;

        try {
            if (mediator instanceof AbstractClassMediator) {
                result = invokeClassMediatorV2(synCtx);
            } else {
                if (hasDynamicProperties) {
                    synchronized (mediator) {
                        result = updateInstancePropertiesAndMediate(synCtx);
                    }
                } else {
                    result = updateInstancePropertiesAndMediate(synCtx);
                }
            }
        } catch (Exception e) {
            // throw Synapse Exception for any exception in class meditor
            // so that the fault handler will be invoked
            throw new SynapseException("Error occured in the mediation of the class mediator", e);
        }

        synLog.traceOrDebug("End : Class mediator");
        
        return result;
    }

    private boolean invokeClassMediatorV2(MessageContext synCtx) {

        List<Object> methodArgs = new ArrayList<>();
        methodArgs.add(synCtx);
        arguments.forEach(arg -> methodArgs.add(getArgument(synCtx, arg.name())));
        Method[] methods = mediator.getClass().getMethods();
        Method targetMethod = null;
        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterCount() == methodArgs.size()) {
                targetMethod = method;
                break;
            }
        }
        if (targetMethod == null) {
            handleException("No suitable method found for name: " + methodName + " with " + methodArgs.size()
                    + " arguments for class " + mediator.getClass().getSimpleName(), synCtx);
        }
        try {
            Object result = targetMethod.invoke(mediator, methodArgs.toArray());
            return Utils.setResultTarget(synCtx, resultTarget, variableName, result);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            handleException("Error while invoking method: " + methodName + " in class "
                    + mediator.getClass().getSimpleName(), e, synCtx);
        } catch (AxisFault e) {
            handleException("Error while setting Class mediator '" + mediator.getClass().getName()
                    + "' result to body", e, synCtx);
        } catch (SynapseException e) {
            handleException("Error while setting Class mediator '" + mediator.getClass().getName()
                    + "' result", e, synCtx);
        }
        return false;
    }

    private Object getArgument(MessageContext context, String argName) {

        if (inputArguments != null && inputArguments.containsKey(argName)) {
            return inputArguments.get(argName).getResolvedArgument(context);
        }
        return null;
    }

    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying class mediator instance for : " + mediator.getClass());
        }
        if (mediator instanceof ManagedLifecycle) {
            ((ManagedLifecycle) mediator).destroy();
        }
    }

    public void init(SynapseEnvironment se) {
        if (log.isDebugEnabled()) {
            log.debug("Initializing class mediator instance for : " + mediator.getClass());
        }
        if (mediator == null) {
            log.warn("init() called before mediator reference set");
            return;
        }

        if (mediator instanceof ManagedLifecycle) {
            ((ManagedLifecycle) mediator).init(se);
        }
    }

    public void setMediator(Mediator mediator) {
		this.mediator = mediator;
	}

	public Mediator getMediator() {
		return mediator;
	}

    public void addProperty(MediatorProperty property) {
        properties.add(property);
    }

    public void addAllProperties(List<MediatorProperty> propertyList) {
	    properties.addAll(propertyList);
        for (MediatorProperty property : properties) {
            SynapsePath expression = property.getExpression();
            // Since expressions such as expression="get-property('...')" or expression="$trp:..." are also dynamic
            // and their values can change with each request, we face a potential race condition when these
            // expressions alter class mediator instance variables using setters. Given that only one instance of the
            // mediator is created and shared among multiple concurrent requests, this shared state can lead to
            // inconsistent behavior and data corruption. Therefore, we need to ensure synchronization not only
            // when the content is altered but also for each evaluation of these dynamic expressions to maintain
            // thread safety and data integrity.
            if (expression != null) {
                hasDynamicProperties = true;
                break;
            }
        }
    }

    public List<MediatorProperty> getProperties() {
        return this.properties;
    }

    @Override
    public boolean isContentAware() {
        return mediator.isContentAware();
    }

    @Override
    public boolean isContentAltering() {
        return true;
    }

    private boolean updateInstancePropertiesAndMediate(MessageContext synCtx) {
	    for (MediatorProperty property : properties) {
            if (property.getExpression() != null) {
                PropertyHelper.setInstanceProperty(property.getName(), property.getEvaluatedExpression(synCtx),
                        mediator);
            }
        }
        return mediator.mediate(synCtx);
    }


    public void setInputArguments(Map<String, InputArgument> inputArguments) {

        this.inputArguments = inputArguments;
    }

    public Map<String, InputArgument> getInputArguments() {

        return inputArguments;
    }

    public void setArguments(List<AbstractClassMediator.Arg> arguments) {

        this.arguments.addAll(arguments);
    }

    public void setResultTarget(String resultTarget) {

        this.resultTarget = resultTarget;
    }

    public String getResultTarget() {

        return resultTarget;
    }

    public void setMethodName(String methodName) {

        this.methodName = methodName;
    }

    public String getMethodName() {

        return methodName;
    }

    public void setVariableName(String variableName) {

        this.variableName = variableName;
    }

    public String getVariableName() {

        return variableName;
    }
}
