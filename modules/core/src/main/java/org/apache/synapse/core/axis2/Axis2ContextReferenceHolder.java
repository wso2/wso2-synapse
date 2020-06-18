/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 */
package org.apache.synapse.core.axis2;

import org.apache.axis2.context.ConfigurationContext;

/**
 * Singleton class to hold Axis2ConfigurationContext.
 */
public class Axis2ContextReferenceHolder {

    private static final Axis2ContextReferenceHolder instance = new Axis2ContextReferenceHolder();
    private ConfigurationContext axis2ConfigurationContext;

    private Axis2ContextReferenceHolder() {

    }

    /**
     * Get instance of Axis2ContextReferenceHolder.
     *
     * @return Axis2ContextReferenceHolder instance
     */
    public static Axis2ContextReferenceHolder getInstance() {
        return instance;
    }

    /**
     * Set Axis2ConfigurationContext.
     *
     * @param axis2ConfigurationContext context to set
     */
    public void setAxis2ConfigurationContext(ConfigurationContext axis2ConfigurationContext) {
        this.axis2ConfigurationContext = axis2ConfigurationContext;
    }

    /**
     * Get axis2 Configuration Context reference.
     *
     * @return ConfigurationContext
     */
    public ConfigurationContext getAxis2ConfigurationContext() {
        return axis2ConfigurationContext;
    }
}
