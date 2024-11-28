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
package org.apache.synapse.endpoints;

import java.util.HashMap;
import java.util.Map;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.core.SynapseEnvironment;
import org.json.JSONObject;

/**
 * Class which defines  custom  user defined endpoints. Custom Endpoint implementations must extend
 * the  <code>AbstractEndpoint</code> class.
 * eg:
 * <endpoint name="CustomEndpoint">
 * 	<class name="org.apache.synapse.endpoint.CustomEndpoint">
 * 		<parameter name="foo">XYZ</parameter>*
 * 	</class>
 * </endpoint>
 */
public class ClassEndpoint extends AbstractEndpoint  {

	/** place to hold list of parameters **/
	private final Map<String, String> parameters = new HashMap<String, String>();
	private Endpoint classEndpoint;
	
	public Map<String, String> getParameters() {
	    return parameters;
    }
	
	public void setParameters(String name,String value) {
	    parameters.put(name, value);
    }
	
	public void setClassEndpoint(Endpoint classEndpoint) {
		this.classEndpoint = classEndpoint;
	}

	public Endpoint getClassEndpoint() {
		return classEndpoint;
	}

	public void send(MessageContext synCtx) {
		if (RuntimeStatisticCollector.isStatisticsEnabled()) {
			Integer currentIndex = null;
			if (getDefinition() != null) {
				currentIndex = OpenEventCollector.reportChildEntryEvent(synCtx, getReportingName(),
						ComponentType.ENDPOINT, getDefinition().getAspectConfiguration(), true);
			}
			try {
				sendMessage(synCtx);
			} finally {
				if (currentIndex != null) {
					CloseEventCollector.closeEntryEvent(synCtx, getReportingName(), ComponentType.MEDIATOR,
							currentIndex, false);
				}
			}
		} else {
			sendMessage(synCtx);
		}
	}

	@Override
	protected void createJsonRepresentation() {
		endpointJson = new JSONObject();
		endpointJson.put(NAME_JSON_ATT, getName());
		endpointJson.put(TYPE_JSON_ATT, "Class Endpoint");
	}

	/**
	 * Override the <code>AbstractEndpoint.init()</code> to load a custom synapse
	 * environment.
	 */
	public void init(SynapseEnvironment synapseEnvironment){
		if (log.isDebugEnabled()) {
			log.debug("Initiate the synapse environment of the class endpoint");		
		}
		try {
			classEndpoint.init(synapseEnvironment);
		} catch (Exception e) {
			throw new SynapseException("Error occured when initiate the class endpoint", e);
		}
	}

	/**
	 * Override the <code>AbstractEndpoint.send()</code> to have a custom
	 * message send out logic.
	 */
	public void sendMessage(MessageContext synMessageContext) {

		logSetter();

		if (log.isDebugEnabled()) {
			log.debug("Start sending message");
			if (log.isTraceEnabled()) {
				log.trace("Message : " + synMessageContext.getEnvelope());
			}
		}
		try {
			classEndpoint.send(synMessageContext);
		} catch (Exception e) {
			throw new SynapseException("Error occured when execute the class endpoint", e);
		}
		
	}
}
