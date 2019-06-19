/*
* Copyright 2005,2006 WSO2, Inc. http://wso2.com
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
import org.apache.synapse.commons.throttle.core.ThrottleUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
		name = "throttle.core.services",
		immediate = true
)
public class ThrottleServiceComponent {

	private static final Log log  = LogFactory.getLog(ThrottleServiceComponent.class.getName());

	@Activate
	protected void activate(ComponentContext context) {
		log.debug("Activating throttle core service component");
		if (ThrottleServiceDataHolder.getInstance().getThrottleProperties() == null){
			ThrottleServiceDataHolder.getInstance().setThrottleProperties(
					ThrottleUtil.loadThrottlePropertiesFromConfigurations());
		}
	}

	@Deactivate
	protected void deactivate(ComponentContext ctx) {
		log.debug("Deactivating throttle core service component");
	}

	/**
	 * Access Hazelcast Instance, which is exposed as an OSGI service.
	 *
	 * @param hazelcastInstance hazelcastInstance found from the OSGI service
	 */
	@Reference(
			name = "throttle.core.services",
			service = com.hazelcast.core.HazelcastInstance.class,
			cardinality = ReferenceCardinality.OPTIONAL,
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
