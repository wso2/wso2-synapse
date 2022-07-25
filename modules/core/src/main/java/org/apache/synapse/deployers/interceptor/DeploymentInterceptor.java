/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.deployers.interceptor;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.axis2.engine.AxisObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.data.ConfigDataHolder;
import org.apache.synapse.registry.Registry;

import java.util.ArrayList;
import java.util.Properties;

/**
 * This deployment interceptor will be called whenever before a module is initialized or service is
 * deployed.
 *
 * @see AxisObserver
 */
public class DeploymentInterceptor implements AxisObserver {

    public static final String SERVICES = "/services/";
    public static final String ACTIVE = "service.active";
    public static final String KEEP_SERVICE_HISTORY_PARAM = "keepServiceHistory";
    private static final Log log = LogFactory.getLog(DeploymentInterceptor.class);
    private static final String SERVICE_GROUPS = "/repository/axis2/service-groups/";
    private Registry synapseRegistry;

    public void init(AxisConfiguration axisConfig) {

    }

    public void serviceGroupUpdate(AxisEvent axisEvent, AxisServiceGroup axisServiceGroup) {
        // This method will not be used.
    }

    public void serviceUpdate(AxisEvent axisEvent, AxisService axisService) {

        if (axisService.isClientSide()) {
            return;
        }
        int eventType = axisEvent.getEventType();
        if (eventType == AxisEvent.SERVICE_DEPLOY) {
            axisService.setActive(getPersistedServiceStatus(axisService));
        } else if (eventType == AxisEvent.SERVICE_STOP) {
            persistServiceStatus(axisService);
        } else if (eventType == AxisEvent.SERVICE_START) {
            removeServiceStatus(axisService);
        } else if (eventType == AxisEvent.SERVICE_REMOVE) {
            log.info("Removing Axis2 Service: " + axisService.getName());
            if (!keepHistory(axisService)) {
                deleteServiceResource(axisService);
            }
        }
    }

    public void moduleUpdate(AxisEvent axisEvent, AxisModule axisModule) {
        // This method will not be used.
    }


    public void addParameter(Parameter parameter) {
        // Not needed.
    }

    public void removeParameter(Parameter param) {
        // Not needed.
    }

    public void deserializeParameters(OMElement omElement) {
        //No need to do anything here
    }

    public Parameter getParameter(String paramName) {

        return null;
    }

    public ArrayList<Parameter> getParameters() {

        return null;
    }

    public boolean isParameterLocked(String paramName) {

        return false;
    }

    private String getServiceResourcePath(AxisService axisService) {

        return SERVICE_GROUPS + axisService.getAxisServiceGroup().getServiceGroupName() + SERVICES + axisService.getName();
    }

    private Registry getRegistry() {

        if (synapseRegistry == null) {
            synapseRegistry = ConfigDataHolder.getInstance().getRegistry();
        }
        return synapseRegistry;
    }

    private boolean getPersistedServiceStatus(AxisService axisService) {

        String serviceResourcePath = getServiceResourcePath(axisService);
        boolean isServerActive = axisService.isActive();
        Properties properties = getRegistry().getResourceProperties(serviceResourcePath);
        if (properties != null) {
            if (properties.getProperty(ACTIVE) != null) {
                isServerActive = Boolean.parseBoolean(properties.getProperty(ACTIVE));
            }
        }
        return isServerActive;
    }

    private void persistServiceStatus(AxisService axisService) {

        String serviceResourcePath = getServiceResourcePath(axisService);
        getRegistry().newNonEmptyResource(serviceResourcePath, false, "text/plain",
                Boolean.toString(axisService.isActive()), ACTIVE);
    }

    private void removeServiceStatus(AxisService axisService) {

        String serviceResourcePath = getServiceResourcePath(axisService);
        if (getRegistry().getResourceProperties(serviceResourcePath) != null) {
            getRegistry().delete(serviceResourcePath);
        }
    }

    private void deleteServiceResource(AxisService axisService) {

        String serviceResourcePath = getServiceResourcePath(axisService);
        if (getRegistry().isResourceExists(serviceResourcePath)) {
            getRegistry().delete(serviceResourcePath);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Service [" + axisService.getName() + "] doesn't have any resource or resource path [" + serviceResourcePath + "] has already been deleted.");
            }
        }
    }

    private boolean keepHistory(AxisService axisService) {

        Parameter keepHistoryParam = axisService.getParameter(KEEP_SERVICE_HISTORY_PARAM);
        if (keepHistoryParam == null) {
            return false;
        }
        Object value = keepHistoryParam.getValue();
        return (value instanceof String && Boolean.parseBoolean((String) value));
    }
}
