/*
 * Copyright (c) 2023, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.commons.throttle.core.internal;

import org.apache.synapse.commons.throttle.core.CallerConfiguration;
import org.apache.synapse.commons.throttle.core.CallerContext;
import org.apache.synapse.commons.throttle.core.RequestContext;
import org.apache.synapse.commons.throttle.core.ThrottleContext;

public interface DistributedThrottleProcessor {

    public boolean canAccessBasedOnUnitTime(CallerContext callerContext, CallerConfiguration configuration,
            ThrottleContext throttleContext, RequestContext requestContext);

    public boolean canAccessIfUnitTimeNotOver(CallerContext callerContext, CallerConfiguration configuration,
            ThrottleContext throttleContext, RequestContext requestContext);

    public boolean canAccessIfUnitTimeOver(CallerContext callerContext, CallerConfiguration configuration,
            ThrottleContext throttleContext, RequestContext requestContext);

    public void syncThrottleCounterParams(CallerContext callerContext, boolean incrementLocalCounter,
            RequestContext requestContext);

    public void syncThrottleWindowParams(CallerContext callerContext, boolean isInvocationFlow);

    public String getType();

    public boolean isEnable();
}
