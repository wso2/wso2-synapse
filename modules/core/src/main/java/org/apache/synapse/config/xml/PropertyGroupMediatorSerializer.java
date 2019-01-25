/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.PropertyGroupMediator;
import org.apache.synapse.mediators.builtin.PropertyMediator;

import java.util.List;

/**
 * <pre>
 * &lt;propertyGroup&gt;
 *   &lt;property name="string" [action=set/remove] (value="literal" | expression="xpath") [type="literal"]&gt;
 *       [Random XML]
 *   &lt;/property&gt;
 * &lt;propertyGroup/&gt*;
 * </pre>
 */
public class PropertyGroupMediatorSerializer extends AbstractMediatorSerializer {

    public OMElement serializeSpecificMediator(Mediator mediator) {
        if (!(mediator instanceof PropertyGroupMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + mediator.getType());
        }
        PropertyGroupMediator propertyGroupMediator = (PropertyGroupMediator) mediator;
        PropertyMediatorSerializer propertyMediatorSerializer = new PropertyMediatorSerializer();
        OMElement propertyGroup = fac.createOMElement(PropertyGroupMediatorFactory.PROPERTY_GROUP_Q);
        saveTracingState(propertyGroup, propertyGroupMediator);
        List<PropertyMediator> propertyGroupList = propertyGroupMediator.getPropGroupList();

        for (PropertyMediator propertyMediator : propertyGroupList) {
            OMElement property = propertyMediatorSerializer.serializeSpecificMediator(propertyMediator);
            propertyGroup.addChild(property);
        }
        return propertyGroup;
    }

    public String getMediatorClassName() {
        return PropertyGroupMediator.class.getName();
    }
}
