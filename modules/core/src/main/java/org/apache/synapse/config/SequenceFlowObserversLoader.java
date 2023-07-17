/**
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.config;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SequenceFlowObserver;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.util.MiscellaneousUtil;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SequenceFlowObserversLoader {

    private static final QName ROOT_Q = new QName("observers");
    private static final QName OBSERVER_Q = new QName("observer");
    private static final QName CLASS_Q = new QName("class");
    private static final QName NAME_ATT = new QName("name");

    private static Log log = LogFactory.getLog(SequenceFlowObserversLoader.class);

    public static List<SequenceFlowObserver> loadObservers() {
        List<SequenceFlowObserver> handlers = new ArrayList<>();
        OMElement observersConfig =
                MiscellaneousUtil.loadXMLConfig(SynapseConstants.SEQUENCE_OBSERVERS_FILE);
        if (observersConfig != null) {

            if (!ROOT_Q.equals(observersConfig.getQName())) {
                handleException("Invalid handler configuration file");
            }

            Iterator iterator = observersConfig.getChildrenWithName(OBSERVER_Q);
            while (iterator.hasNext()) {
                OMElement observerElem = (OMElement) iterator.next();

                String name = null;
                if (observerElem.getAttribute(NAME_ATT) != null) {
                    name = observerElem.getAttributeValue(NAME_ATT);
                } else {
                    handleException("Name not defined in one or more handlers");
                }

                if (observerElem.getAttribute(CLASS_Q) != null) {
                    String className = observerElem.getAttributeValue(CLASS_Q);
                    if (!"".equals(className)) {
                        SequenceFlowObserver observer = createObserver(className);
                        if (observer != null) {
                            handlers.add(observer);
                            observer.setName(name);
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

    private static SequenceFlowObserver createObserver(String classFQName) {
        Object obj = null;
        try {
            obj = Class.forName(classFQName).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            handleException("Error creating Handler for class name : " + classFQName, e);
        }

        if (obj instanceof SequenceFlowObserver) {
            return (SequenceFlowObserver) obj;
        } else {
            handleException("Error creating Handler. The Handler should be of type " +
                    "org.apache.synapse.Handler");
        }
        return null;
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
