/*
 *  Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.synapse.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.throttle.core.ConcurrentAccessController;
import org.apache.synapse.commons.throttle.core.ConcurrentAccessReplicator;

/**
 * Utility class with related Synapse concurrency throttling.
 */
public class ConcurrencyThrottlingUtils {

    private static final Log log = LogFactory.getLog(ConcurrencyThrottlingUtils.class);

    /**
     * Decrement the internal counter for concurrency throttling, in case of normal response retrieval,
     * delayed response at synapse timeout, exceptional cases while mediation end up trigger fault.
     * In clustered environment, replicate the decremented value to other members.
     *
     * @param synCtx Synapse Message Context of which mediation occurs.
     */
    public static void decrementConcurrencyThrottleAccessController(MessageContext synCtx) {

        Boolean isConcurrencyThrottleEnabled = (Boolean) synCtx
                .getProperty(SynapseConstants.SYNAPSE_CONCURRENCY_THROTTLE);

        if (isConcurrencyThrottleEnabled != null && isConcurrencyThrottleEnabled) {
            ConcurrentAccessController concurrentAccessController = (ConcurrentAccessController) synCtx
                    .getProperty(SynapseConstants.SYNAPSE_CONCURRENT_ACCESS_CONTROLLER);
            int available = concurrentAccessController.incrementAndGet();
            int concurrentLimit = concurrentAccessController.getLimit();
            if (log.isDebugEnabled()) {
                log.debug("Concurrency Throttle : Connection returned" + " :: " + available + " of available of"
                        + concurrentLimit + " connections");
            }
            ConcurrentAccessReplicator concurrentAccessReplicator = (ConcurrentAccessReplicator) synCtx
                    .getProperty(SynapseConstants.SYNAPSE_CONCURRENT_ACCESS_REPLICATOR);
            String throttleKey = (String) synCtx.getProperty(SynapseConstants.SYNAPSE_CONCURRENCY_THROTTLE_KEY);
            if (concurrentAccessReplicator != null) {
                concurrentAccessReplicator.replicate(throttleKey, concurrentAccessController);
            }
            //once decremented, clear the flag since we no longer required to decrement the value
            //concurrency throttle access controller
            synCtx.setProperty(SynapseConstants.SYNAPSE_CONCURRENCY_THROTTLE, false);
        }

    }

}
