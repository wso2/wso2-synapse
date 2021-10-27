/*
 * Copyright 2021 WSO2, Inc. http://wso2.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package org.apache.synapse.commons.throttle.core.internal;

import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.throttle.core.DistributedCounterManager;
import org.apache.synapse.commons.throttle.core.HazelcastDistributedCounterManager;
import org.apache.synapse.commons.throttle.core.ThrottleDistributedInstancesCleanupTask;
import org.apache.synapse.commons.throttle.core.ThrottleUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
        name = "hazelcast.throttle.core.services",
        immediate = true
)
public class HazelCastBasedThrottleServiceComponent {

    private static final Log log = LogFactory.getLog(HazelCastBasedThrottleServiceComponent.class.getName());
    ThrottleDistributedInstancesCleanupTask distributedObjectsCleanupTask;

    @Activate
    protected void activate(ComponentContext context) {

        log.debug("Activating HazelCast base throttle core service component");
        DistributedCounterManager distributedCounterManager = new HazelcastDistributedCounterManager();
        context.getBundleContext().registerService(DistributedCounterManager.class, distributedCounterManager, null);
        distributedObjectsCleanupTask = new ThrottleDistributedInstancesCleanupTask();
        distributedObjectsCleanupTask.start();
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {

        distributedObjectsCleanupTask.stop();
        log.debug("Deactivating throttle core service component");
    }

    /**
     * Access Hazelcast Instance, which is exposed as an OSGI service.
     *
     * @param hazelcastInstance hazelcastInstance found from the OSGI service
     */
    @Reference(
            name = "hazelcast.distributed.instance.service",
            service = HazelcastInstance.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetHazelcastInstance"
    )
    protected void setHazelcastInstance(HazelcastInstance hazelcastInstance) {

        ThrottleServiceDataHolder.getInstance().setHazelCastInstance(hazelcastInstance);
    }

    protected void unsetHazelcastInstance(HazelcastInstance hazelcastInstance) {

        ThrottleServiceDataHolder.getInstance().setHazelCastInstance(null);
    }
}
