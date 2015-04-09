/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.nhttp.util.dynamicconfigurations;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.ParameterInclude;

/**
 * Interface to be implemented by all SSL senders and listeners in order to receive notifications on config file
 * changes at runtime
 */
public interface SSLProfileLoader {

    /**
     * Reload SSL configurations by each Listener/sender
     *
     * @param transport Transport In/Out Description
     * @throws AxisFault
     */
    public void reloadConfig(ParameterInclude transport) throws AxisFault;
}
