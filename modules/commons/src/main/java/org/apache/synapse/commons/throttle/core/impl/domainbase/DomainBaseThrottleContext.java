/*
*  Licensed to the Apache Software Foundation (ASF) under one
*  or more contributor license agreements.  See the NOTICE file
*  distributed with this work for additional information
*  regarding copyright ownership.  The ASF licenses this file
*  to you under the Apache License, Version 2.0 (the
*  "License"); you may not use this file except in compliance
*  with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.commons.throttle.core.impl.domainbase;

import org.apache.synapse.commons.throttle.core.ThrottleConfiguration;
import org.apache.synapse.commons.throttle.core.ThrottleConstants;
import org.apache.synapse.commons.throttle.core.ThrottleContext;
import org.apache.synapse.commons.throttle.core.ThrottleReplicator;

/**
 * Holds all the run time data for all domain based  remote callers
 */

public class DomainBaseThrottleContext extends ThrottleContext {

    public DomainBaseThrottleContext(ThrottleConfiguration throttleConfiguration,
                                     ThrottleReplicator replicator) {
        super(throttleConfiguration, replicator);
    }

    public int getType() {
        return ThrottleConstants.DOMAIN_BASE;
    }

}
