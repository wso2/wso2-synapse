package org.apache.synapse.core.axis2;
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

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.dispatchers.AbstractServiceDispatcher;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.util.LoggingControl;
import org.apache.axis2.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.versioning.VersionComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionedProxyServiceDispatcher extends AbstractServiceDispatcher {

	public static final String NAME = "VersionedProxyServiceDispatcher";
	private static final Log log = LogFactory.getLog(VersionedProxyServiceDispatcher.class);
	private AxisConfiguration registry;
	private static final Pattern VERSION_PATTERN =
			Pattern.compile("[\\w/.]+[/][\\d]+[.][\\d]+[.][\\d-]+");

	public VersionedProxyServiceDispatcher() {
	}

	public AxisService findService(MessageContext messageContext) throws AxisFault {
		EndpointReference toEPR = messageContext.getTo();
		if (toEPR == null) {
			if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
				log.debug(messageContext.getLogIDString() +
				          " Attempted to check for Service using null target endpoint URI");
			}

			return null;
		} else {
			if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
				log.debug(messageContext.getLogIDString() +
				          " Checking for Service using target endpoint address : " +
				          toEPR.getAddress());
			}

			String filePart = toEPR.getAddress();
			ConfigurationContext configurationContext = messageContext.getConfigurationContext();
			String serviceOpPart = Utils.getServiceAndOperationPart(filePart, messageContext
					.getConfigurationContext().getServiceContextPath());
			if (serviceOpPart == null) {
				if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
					log.debug(messageContext.getLogIDString() +
					          " Attempted to check for Service using target endpoint URI, but the service fragment was missing");
				}

				return null;
			} else {
				registry = configurationContext.getAxisConfiguration();
				String[] parts = serviceOpPart.split("/");
				String serviceName;
				AxisServiceWrapper service;

				boolean version = false;

				if (versionAvailable(serviceOpPart)) {
					version = true;
				}

				service = findService(serviceOpPart);
				AxisService axisService = service.axisService;

				if (axisService == null && !version) {
					Map<String, AxisService> services = registry.getServices();
					List<String> names = new ArrayList();
					String name = parts[0];

					if (name.contains(".")) {
						name = name.split("\\.")[0];
					}

					SynapseConfiguration synCfg = null;
					Parameter synCfgParam = registry.getParameter(SynapseConstants.SYNAPSE_CONFIG);
					if (synCfgParam != null) {
						synCfg = (SynapseConfiguration) synCfgParam.getValue();
					}
					if (synCfg != null) {
						String defaultProxy = synCfg.getDefaultProxyKey(name);
						if (defaultProxy != null) {
							service = findService(serviceOpPart.replace(name, defaultProxy));
							axisService = service.axisService;
						}
					}

					if (axisService == null) {
						for (String key : services.keySet()) {
							if (key.contains(name)) {

								if (versionAvailable(key)) {
									names.add(key);
								}
							}
						}
						if (names.size() == 1) {
							serviceName = names.get(0);
							axisService = registry.getService(serviceName);
						} else if (names.size() != 0) {
							//find highest version
							Collections.sort(names, new VersionComparator());
							serviceName = names.get(names.size() - 1);
							service = findService(serviceOpPart.replace(name, serviceName));
							axisService = service.axisService;
						}
					}

				}

				if (axisService != null) {
					Map endpoints = axisService.getEndpoints();
					if (endpoints != null) {
						if (endpoints.size() == 1) {
							messageContext.setProperty("endpoint", endpoints
									.get(axisService.getEndpointName()));
						} else {
							serviceName = axisService.getName();
							if (service.serviceName.contains(serviceName + ".")) {
								String endpointName = serviceOpPart
										.substring(serviceOpPart.indexOf(serviceName + ".") +
										           serviceName.length() + 1);
								messageContext.setProperty("endpoint", endpoints.get(endpointName));
							} else {
								messageContext.setProperty("endpoint", endpoints
										.get(axisService.getEndpointName()));
							}
						}
					}
				}

				return axisService;
			}
		}
	}

	public void initDispatcher() {
		this.init(new HandlerDescription("VersionedProxyServiceDispatcher"));
	}

	private AxisServiceWrapper findService(String serviceOpPart) throws AxisFault {

		int nameLength = 0;
		AxisService axisService = null;
		String[] parts = serviceOpPart.split("/");
		String serviceName = "";

		if (versionAvailable(serviceOpPart)) {
			nameLength++;
		}

		for (int count = 0; axisService == null && count < parts.length &&
		                    count < Constants.MAX_HIERARCHICAL_DEPTH; ++count) {
			serviceName =
					count == 0 ? serviceName + parts[count] : serviceName + "/" + parts[count];
			if (count >= nameLength) {
				axisService = registry.getService(serviceName);
			}
		}
		return new AxisServiceWrapper(axisService, serviceName);
	}

	/**
	 * Returns true if version exists in String;
	 *
	 * @param serviceOperationPart String to be searched for version definition.
	 * @return boolean
	 */
	public boolean versionAvailable(String serviceOperationPart) {
		Matcher matcher = VERSION_PATTERN.matcher(serviceOperationPart);
		return matcher.matches();
	}

	private class AxisServiceWrapper {
		final AxisService axisService;
		final String serviceName;

		AxisServiceWrapper(AxisService axisService, String serviceName) {
			this.axisService = axisService;
			this.serviceName = serviceName;
		}
	}

}