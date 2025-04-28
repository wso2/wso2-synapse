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

package com.synapse.core.deployers;


import com.synapse.core.artifacts.mediators.LogMediator;
import com.synapse.core.artifacts.Mediator;
import com.synapse.core.artifacts.utils.Position;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;

import javax.xml.namespace.QName;

public class LogMediatorDeployer implements MediatorDeployer {

    @Override
    public Mediator unmarshal(OMElement element, Position position) throws Exception {
        if (!"log".equals(element.getLocalName())) {
            throw new Exception("Invalid element, expected <log> but got <" + element.getLocalName() + ">");
        }

        OMAttribute categoryAttr = element.getAttribute(new QName("category"));
        String category = (categoryAttr != null) ? categoryAttr.getAttributeValue() : "";

        OMElement messageElement = element.getFirstChildWithName(new QName("message"));
        String message = (messageElement != null) ? messageElement.getText() : "";

        position.setHierarchy(position.getHierarchy() + "->log");

        return new LogMediator(category, message, position);
    }

}
