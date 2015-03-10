/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.transport.passthru.core.carbonext;


import org.apache.log4j.Logger;
import sun.misc.Service;

import java.util.Iterator;
import java.util.Map;

public final class TenantInfoProvider {

    private static final Logger log = Logger.getLogger(TenantInfoProvider.class);

    /**
     * Provide teanant information of the calling tenant.
     * @return
     */
    public static Map<String, String> getTenantInformation() {
        if (log.isDebugEnabled()) {
            log.debug("Trying to fetch TenantInfoHolder from classpath.. ");
        }
        Iterator<TenantInfoHolder> it = Service.providers(TenantInfoHolder.class);

        while (it.hasNext()) {
            TenantInfoHolder tenantInfoHolder = it.next();

            Map<String, String> tenantInformation = tenantInfoHolder.getTenantInformation();
            if (tenantInformation != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot find Tenant Information");
                }
                return tenantInformation;
            }
        }
        return null;
    }

}
