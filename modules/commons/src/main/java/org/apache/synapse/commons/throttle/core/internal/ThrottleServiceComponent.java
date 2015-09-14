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
import org.osgi.service.component.ComponentContext;

/**
 * This class represents the throttle core declarative service component.
 *
 * @scr.component name="throttle.core.services" immediate="true"
 * @scr.reference name="hazelcast.instance.service"
 * interface="com.hazelcast.core.HazelcastInstance"
 * cardinality="0..1"
 * policy="dynamic"
 * bind="setHazelcastInstance"
 * unbind="unsetHazelcastInstance"
 */
public class ThrottleServiceComponent {

	private static final Log log  = LogFactory.getLog(ThrottleServiceComponent.class.getName());

	protected void activate(ComponentContext context) {
		log.debug("Activating throttle core service component");
	}

	protected void deactivate(ComponentContext ctx) {
		log.debug("Deactivating throttle core service component");
	}

	/**
	 * Access Hazelcast Instance, which is exposed as an OSGI service.
	 *
	 * @param hazelcastInstance hazelcastInstance found from the OSGI service
	 */
	protected void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
		ThrottleServiceDataHolder.getInstance().setHazelCastInstance(hazelcastInstance);
	}

	protected void unsetHazelcastInstance(HazelcastInstance hazelcastInstance) {
		ThrottleServiceDataHolder.getInstance().setHazelCastInstance(null);
	}
}
