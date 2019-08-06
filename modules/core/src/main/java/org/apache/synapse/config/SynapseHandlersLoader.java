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
package org.apache.synapse.config;


import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseHandler;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.util.MiscellaneousUtil;
import org.apache.synapse.commons.util.PropertyHelper;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class to load synapse handlers to the synapse environment
 */
public class SynapseHandlersLoader {


    private static final QName ROOT_Q = new QName("handlers");
    private static final QName HANDLER_Q = new QName("handler");
    private static final QName CLASS_Q = new QName("class");
    private static final QName NAME_ATT = new QName("name");
    private static final QName PARAM_Q = new QName("parameter");
    private static final QName VALUE_ATT = new QName("value");

    private static Log log = LogFactory.getLog(SynapseHandlersLoader.class);

    /**
     * Load and get all synapse handlers
     *
     * @return List of loaded synapse handlers
     */
    public static List<SynapseHandler> loadHandlers() {
        List<SynapseHandler> handlers = new ArrayList<>();
        OMElement handlersConfig =
                MiscellaneousUtil.loadXMLConfig(SynapseConstants.SYNAPSE_HANDLER_FILE);
        if (handlersConfig != null) {

            if (!ROOT_Q.equals(handlersConfig.getQName())) {
                handleException("Invalid handler configuration file");
            }

            Iterator iterator = handlersConfig.getChildrenWithName(HANDLER_Q);
            while (iterator.hasNext()) {
                OMElement handlerElem = (OMElement) iterator.next();

                String name = null;
                if (handlerElem.getAttribute(NAME_ATT) != null) {
                    name = handlerElem.getAttributeValue(NAME_ATT);
                } else {
                    handleException("Name not defined in one or more handlers");
                }

                if (handlerElem.getAttribute(CLASS_Q) != null) {
                    String className = handlerElem.getAttributeValue(CLASS_Q);
                    if (!"".equals(className)) {
                        SynapseHandler handler = createHandler(className);
                        if (handler != null) {
                            handlers.add(handler);
                            handler.setName(name);
                            populateParameters(handlerElem, handler);
                        }
                    } else {
                        handleException("Class name is null for handle name : " + name);
                    }
                } else {
                    handleException("Class name not defined for handler named : " + name);
                }

            }
        }
        return handlers;
    }

    private static SynapseHandler createHandler(String classFQName) {
        Object obj = null;
        try {
            obj = Class.forName(classFQName).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            handleException("Error creating JaegerSpanHandler for class name : " + classFQName, e);
        }

        if (obj instanceof SynapseHandler) {
            return (SynapseHandler) obj;
        } else {
            handleException("Error creating JaegerSpanHandler. The JaegerSpanHandler should be of type " +
                            "org.apache.synapse.JaegerSpanHandler");
        }
        return null;
    }

    private static void populateParameters(OMElement handlerElem, SynapseHandler handler) {

        for (Iterator it = handlerElem.getChildrenWithName(PARAM_Q); it.hasNext();) {
            OMElement child = (OMElement) it.next();

            String propName = child.getAttribute(NAME_ATT).getAttributeValue();
            if (propName == null) {
                handleException("Synapse JaegerSpanHandler parameter must contain the name attribute");
            } else {
                if (child.getAttribute(VALUE_ATT) != null) {
                    String value = child.getAttribute(VALUE_ATT).getAttributeValue();
                    handler.addProperty(propName, value);
                    PropertyHelper.setInstanceProperty(propName, value, handler);
                } else {
                    OMNode omElt = child.getFirstElement();
                    if (omElt != null) {
                        handler.addProperty(propName, omElt);
                        PropertyHelper.setInstanceProperty(propName, omElt, handler);
                    } else {
                        handleException("Synapse JaegerSpanHandler parameter must contain " +
                                        "name and value attributes, or a name and a child XML fragment");
                    }
                }
            }
        }
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception ex) {
        log.error(msg, ex);
        throw new SynapseException(msg, ex);
    }

}
