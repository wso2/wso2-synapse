/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.commons.resolvers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.property.PropertyHolder;

/**
 * Config Resolver can be used to resolve configurable property variables in the synapse config.
 */
public class ConfigResolver implements Resolver {

    private static final Log LOG = LogFactory.getLog(FilePropertyResolver.class);

    private String input;

    @Override
    public void setVariable(String input) {
        this.input = input;
    }

    @Override
    public String resolve() {
        String propertyValue = PropertyHolder.getInstance().getPropertyValue(this.input);
        if (propertyValue == null) {
            throw new ResolverException("Parameter key: " + input + " could not be found");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Resolving parameter key: "+ input + " value: " + propertyValue);
        }
        return propertyValue;
    }
}
