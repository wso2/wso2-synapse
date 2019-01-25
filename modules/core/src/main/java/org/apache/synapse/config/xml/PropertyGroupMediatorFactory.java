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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.xml.namespace.QName;

/**
 * Creates a propertyGroup mediator through the supplied XML configuration
 * <p/>
 * <pre>
 * &lt;propertyGroup&gt;
 *   &lt;property name="string" [action=set/remove] (value="literal" | expression="xpath")
 *       [scope=(axis2 | axis2-client | transport)]/&gt;*
 *   &lt;propertyGroup/&gt;
 * </pre>
 */
public class PropertyGroupMediatorFactory extends AbstractMediatorFactory {

    public Mediator createSpecificMediator(OMElement elem, Properties properties) {
        PropertyGroupMediator propertyGroupMediator = new PropertyGroupMediator();
        PropertyMediatorFactory propertyMediatorFactory = new PropertyMediatorFactory();
        List<PropertyMediator> propertyGroupList = new ArrayList<>();
        Iterator propertyGroupIterator = elem.getChildrenWithName(PROP_Q);

        while (propertyGroupIterator.hasNext()) {
            OMElement propertyElement = (OMElement) propertyGroupIterator.next();
            Mediator propertyMediator = propertyMediatorFactory.createSpecificMediator(propertyElement, properties);
            propertyGroupList.add((PropertyMediator) propertyMediator);
        }
        propertyGroupMediator.setPropGroupList(propertyGroupList);
        return propertyGroupMediator;
    }

    public QName getTagQName() {
        return PROPERTY_GROUP_Q;
    }
}
