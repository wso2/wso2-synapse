/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.synapse.core.artifacts.api;

import com.synapse.core.artifacts.ConfigContext;
import com.synapse.core.artifacts.api.API;
import com.synapse.core.artifacts.api.CORSConfig;
import com.synapse.core.artifacts.api.Resource;
import com.synapse.core.ports.InboundMessageMediator;
import com.synapse.core.synctx.MsgContext;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

public class APIDeploymentService {
    
    private static final Logger log = LogManager.getLogger(APIDeploymentService.class);
    private final ConfigurationContext axisConfigContext;
    private final InboundMessageMediator mediator;
    private final Map<String, AxisService> deployedServices = new HashMap<>();

    public APIDeploymentService(ConfigurationContext axisConfigContext, InboundMessageMediator mediator) {
        this.axisConfigContext = axisConfigContext;
        this.mediator = mediator;
    }

    public void deployAPIs() {
        ConfigContext configContext = ConfigContext.getInstance();
        Map<String, API> apiMap = configContext.getApiMap();
        
        for (API api : apiMap.values()) {
            try {
                deployAPI(api);
                log.info("Successfully deployed API: {}", api.getName());
            } catch (Exception e) {
                log.error("Failed to deploy API: {}", api.getName(), e);
            }
        }
    }

    public void deployAPI(API api) throws Exception {
        String serviceName = api.getName() + "Service";
        
        // Create a new AxisService
        AxisService service = new AxisService(serviceName);
        service.setClassLoader(this.getClass().getClassLoader());
        
        // Set service path to match API context
        service.setName(serviceName);
        service.addParameter("ServiceClass", APIMessageReceiver.class.getName());
        service.addParameter("api-context", api.getContext());
        
        // Add CORS configuration if present
        if (api.getCorsConfig() != null && api.getCorsConfig().isEnabled()) {
            addCORSConfiguration(service, api.getCorsConfig());
        }
        
        // For each resource, create an operation
        for (Resource resource : api.getResources()) {
            addResourceOperation(service, resource, api.getContext());
        }
        
        // Deploy the service
        AxisConfiguration axisConfig = axisConfigContext.getAxisConfiguration();
        axisConfig.addService(service);
        
        // Store the service for later reference
        deployedServices.put(api.getName(), service);
    }

    private void addCORSConfiguration(AxisService service, CORSConfig corsConfig) {
        try {
            service.addParameter("cors-enabled", String.valueOf(corsConfig.isEnabled()));
        } catch (AxisFault e) {
            e.printStackTrace();
        }
        
        if (corsConfig.getAllowOrigins() != null) {
            try {
                service.addParameter("cors-allow-origins", corsConfig.getAllowOrigins());
            } catch (AxisFault e) {
                e.printStackTrace();
            }
        }
        
        if (corsConfig.getAllowMethods() != null) {
            try {
                service.addParameter("cors-allow-methods", corsConfig.getAllowMethods());
            } catch (AxisFault e) {
                e.printStackTrace();
            }
        }
        
        if (corsConfig.getAllowHeaders() != null) {
            try {
                service.addParameter("cors-allow-headers", corsConfig.getAllowHeaders());
            } catch (AxisFault e) {
                e.printStackTrace();
            }
        }
        
        if (corsConfig.getExposeHeaders() != null) {
            try {
                service.addParameter("cors-expose-headers", corsConfig.getExposeHeaders());
            } catch (AxisFault e) {
                e.printStackTrace();
            }
        }
        
        try {
            service.addParameter("cors-allow-credentials", String.valueOf(corsConfig.isAllowCredentials()));
        } catch (AxisFault e) {
            e.printStackTrace();
        }
        try {
            service.addParameter("cors-max-age", String.valueOf(corsConfig.getMaxAge()));
        } catch (AxisFault e) {
            e.printStackTrace();
        }
    }

    private void addResourceOperation(AxisService service, Resource resource, String apiContext) {
        // Create an operation based on the resource
        String operationName = generateOperationName(resource.getMethods(), resource.getUriTemplate());
        QName opName = new QName(operationName);
        
        // Create an operation and set its details
        AxisOperation operation = new InOutAxisOperation(opName);
        operation.setMessageReceiver(new APIMessageReceiver(resource, apiContext, mediator));
        
        // Set HTTP method binding
        for (String method : resource.getMethods().split(",")) {
            try {
                operation.addParameter(
                    new org.apache.axis2.description.Parameter(
                        HTTPConstants.HTTP_METHOD + "_" + method.trim().toUpperCase(), 
                        resource.getUriTemplate()
                    )
                );
            } catch (AxisFault e) {
                e.printStackTrace();
            }
        }
        
        // Add the operation to the service
        try {
            service.addOperation(operation);
        } catch (Exception e) {
            log.error("Failed to add operation for resource: {}", resource.getUriTemplate(), e);
        }
    }

    private String generateOperationName(String methods, String uriTemplate) {
        // Create a unique operation name based on methods and URI template
        String normalizedUri = uriTemplate.replace("/", "_").replace("{", "").replace("}", "");
        return methods.replace(",", "_") + "_" + normalizedUri;
    }

    public void undeployAPI(String apiName) {
        AxisService service = deployedServices.get(apiName);
        if (service != null) {
            try {
                AxisConfiguration axisConfig = axisConfigContext.getAxisConfiguration();
                axisConfig.removeService(service.getName());
                deployedServices.remove(apiName);
                log.info("Successfully undeployed API: {}", apiName);
            } catch (Exception e) {
                log.error("Failed to undeploy API: {}", apiName, e);
            }
        }
    }
}