/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.transport.passthru.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.util.MiscellaneousUtil;
import org.apache.synapse.commons.util.PropertyHelper;
import org.apache.synapse.transport.passthru.StreamInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.xml.namespace.QName;

public class StreamInterceptorsLoader {

    StreamInterceptorsLoader() {
    }

    private static Log log = LogFactory.getLog(StreamInterceptorsLoader.class);

    private static final QName ROOT_Q = new QName("interceptors");
    private static final QName INTERCEPTOR_Q = new QName("interceptor");
    private static final QName CLASS_Q = new QName("class");
    private static final QName NAME_ATT = new QName("name");
    private static final QName PARAM_Q = new QName("parameter");
    private static final QName VALUE_ATT = new QName("value");

    private static List<StreamInterceptor> interceptors = new ArrayList<>();

    private static boolean isLoadedAlready = false;

    private static Lock lock = new ReentrantLock();

    /**
     * Load and get all synapse interceptors
     *
     * @return List of loaded synapse interceptors
     */
    public static List<StreamInterceptor> getInterceptors() {

        if (!isLoadedAlready) {
            try {
                lock.lock();
                if (!isLoadedAlready) {
                    loadInterceptors();
                    isLoadedAlready = true;
                }
            } finally {
                lock.unlock();
            }
        }
        return Collections.unmodifiableList(interceptors);
    }

    private static void loadInterceptors() {

        OMElement interceptorsConfig = MiscellaneousUtil.loadXMLConfig(RelayConstants.STREAM_INTERCEPTOR_FILE, false);
        if (interceptorsConfig != null) {

            if (!ROOT_Q.equals(interceptorsConfig.getQName())) {
                handleException("Invalid interceptor configuration file");
            }

            Iterator iterator = interceptorsConfig.getChildrenWithName(INTERCEPTOR_Q);
            while (iterator.hasNext()) {
                OMElement interceptorElem = (OMElement) iterator.next();
                if (interceptorElem.getAttribute(CLASS_Q) != null) {
                    String className = interceptorElem.getAttributeValue(CLASS_Q);
                    if (!"".equals(className)) {
                        StreamInterceptor interceptor = createInterceptor(className);
                        interceptors.add(interceptor);
                        populateParameters(interceptorElem, interceptor);
                    } else {
                        handleException("Class name is one or more interceptor");
                    }
                } else {
                    handleException("Class name is one or more interceptor");
                }
            }
        }
    }

    private static StreamInterceptor createInterceptor(String classFQName) {

        Object obj = null;
        try {
            obj = Class.forName(classFQName).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            handleException("Error creating Interceptor for class name : " + classFQName, e);
        }
        if (obj instanceof StreamInterceptor) {
            return (StreamInterceptor) obj;
        } else {
            handleException(
                    "Error creating StreamInterceptor. The Interceptor should be of type org.apache.synapse.transport.passthru.StreamInterceptor");
        }
        return null;
    }

    private static void populateParameters(OMElement handlerElem, StreamInterceptor interceptor) {

        for (Iterator it = handlerElem.getChildrenWithName(PARAM_Q); it.hasNext(); ) {
            OMElement child = (OMElement) it.next();

            String propName = child.getAttribute(NAME_ATT).getAttributeValue();
            if (propName == null) {
                handleException("StreamInterceptor parameter must contain the name attribute");
            } else {
                if (child.getAttribute(VALUE_ATT) != null) {
                    String value = child.getAttribute(VALUE_ATT).getAttributeValue();
                    interceptor.addProperty(propName, value);
                    PropertyHelper.setInstanceProperty(propName, value, interceptor);
                } else {
                    OMNode omElt = child.getFirstElement();
                    if (omElt != null) {
                        interceptor.addProperty(propName, omElt);
                        PropertyHelper.setInstanceProperty(propName, omElt, interceptor);
                    } else {
                        handleException(
                                "StreamInterceptor parameter must contain name and value attributes, or a name and a child XML fragment");
                    }
                }
            }
        }
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new RuntimeException(msg);
    }

    private static void handleException(String msg, Exception ex) {
        log.error(msg, ex);
        throw new RuntimeException(msg, ex);
    }
}
