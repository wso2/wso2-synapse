/**
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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

package org.apache.synapse.commons.util.ext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.util.MiscellaneousUtil;

import java.util.Properties;

public final class TenantInfoInitiatorProvider {
    private static final Log logger = LogFactory.getLog(TenantInfoInitiator.class.getName());

    public static final String CARBON_TENANT_INFO_INITIATOR = "synapse.carbon.ext.tenant.info.initiator";
    public static final String PROPERTY_FILE_PATH = "synapse.properties";

    private static TenantInfoInitiator TenantInfoInitiatorInstance = null;

    private TenantInfoInitiatorProvider(){
    }

    /**
     * Get instance of TenantInfoInitiator loaded based on synapse property path
     *
     * @return TenantInfoInitiator instance
     */
    public static TenantInfoInitiator getTenantInfoInitiator() {
        if (TenantInfoInitiatorInstance == null) {
            try {
                Properties properties = MiscellaneousUtil.loadProperties(PROPERTY_FILE_PATH);
                String property = properties.getProperty(CARBON_TENANT_INFO_INITIATOR);
                if (property != null) {
                    Class clazz = TenantInfoInitiator.class.getClassLoader().
                            loadClass(property.trim());
                    TenantInfoInitiatorInstance = (TenantInfoInitiator) clazz.newInstance();
                }
            } catch (Exception e) {
                logger.error("Error while initializing tenant info configuration provider. Error:"
                             + e.getLocalizedMessage());
            }
        }
        return TenantInfoInitiatorInstance;
    }
}