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

package org.apache.synapse.mediators.builtin;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import java.util.ArrayList;
import java.util.List;

/**
 * The propertyGroup mediator would save(or remove) a set of named properties as a local property of
 * the Synapse Message Context or as a property of the Axis2 Message Context or
 * as a Transport Header.
 * Properties set this way could be extracted through the XPath extension function
 * "synapse:get-property(scope,prop-name)"
 */
public class PropertyGroupMediator extends AbstractMediator {

    /**
     * The list which contain properties inside the propertyGroup
     */
    private List<PropertyMediator> propGroupList = new ArrayList<>();

    /**
     * Sets the properties into the current (local) Synapse Context or into the Axis Message Context
     * or into Transports Header and removes above properties from the corresponding locations.
     *
     * @param synCtx the message context
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {
        for (PropertyMediator propertyMediator : propGroupList) {
            propertyMediator.mediate(synCtx);
        }
        return true;
    }

    public List<PropertyMediator> getPropGroupList() {
        return propGroupList;
    }

    public void setPropGroupList(List<PropertyMediator> propGroupList) {
        this.propGroupList = propGroupList;
    }
}
