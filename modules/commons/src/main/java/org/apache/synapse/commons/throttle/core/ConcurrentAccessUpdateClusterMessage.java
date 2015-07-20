/*
* Copyright 2014 WSO2, Inc. http://wso2.com
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

package org.apache.synapse.commons.throttle.core;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConcurrentAccessUpdateClusterMessage extends ClusteringMessage {

    private static final Log log = LogFactory.getLog(ConcurrentAccessUpdateClusterMessage.class);
    private String key;
    private ConcurrentAccessController concurrentAccessController;

    public ConcurrentAccessUpdateClusterMessage(String key,
                                           ConcurrentAccessController concurrentAccessController) {
        this.key = key;
        this.concurrentAccessController = concurrentAccessController;
        log.debug("Initializing with ConcurrentAccessController : " + key + " " + this.getUuid());
    }

    @Override
    public ClusteringCommand getResponse() {
        return null;
    }

    @Override
    public void execute(ConfigurationContext configContext) throws ClusteringFault {
        if (log.isDebugEnabled()) {
            log.debug("Received ConcurrentAccessUpdateClusterMessage : " + this.getUuid());
        }
        ThrottleDataHolder throttleDataHolder =
                (ThrottleDataHolder) configContext.getProperty(ThrottleConstants.THROTTLE_INFO_KEY);
        if (throttleDataHolder != null) {
            if (log.isTraceEnabled()) {
                log.trace("Start executing ClusterMessage : " + this.getUuid());
            }
            ConcurrentAccessController localAccessController =
                    throttleDataHolder.getConcurrentAccessController(key);
            if (log.isDebugEnabled()) {
                log.debug("Getting local ConcurrentAccessController for key : " + key);
            }
            if (localAccessController == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Local ConcurrentAccessController for key : " + key +
                            " is not present in ThrottleDataHolder");
                }
                synchronized (key.intern()) {
                    localAccessController = throttleDataHolder.getConcurrentAccessController(key);
                    if (localAccessController == null) {
                        throttleDataHolder
                                .setConcurrentAccessController(key, concurrentAccessController);
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Replicated the ConcurrentAccessController for key : " + key +
                            " in ThrottleDataHolder");
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Local ConcurrentAccessController for key : " + key +
                            " is already present in ThrottleDataHolder");
                }
                synchronized (key.intern()) {
                    localAccessController = throttleDataHolder.getConcurrentAccessController(key);
                    if (localAccessController != null) {
                        throttleDataHolder.removeConcurrentAccessController(key);
                        throttleDataHolder
                                .setConcurrentAccessController(key, concurrentAccessController);
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Replicated the ConcurrentAccessController for key : " + key +
                            " in ThrottleDataHolder");
                }
            }
            if (log.isTraceEnabled()) {
                log.trace("Finished executing ClusterMessage : " + this.getUuid());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("ThrottleDataHolder is not present in current ConfigurationContext");
            }
            synchronized (configContext) {
                throttleDataHolder = (ThrottleDataHolder) configContext
                        .getProperty(ThrottleConstants.THROTTLE_INFO_KEY);
                if (throttleDataHolder == null) {
                    throttleDataHolder = new ThrottleDataHolder();
                    throttleDataHolder
                            .setConcurrentAccessController(key, concurrentAccessController);
                    configContext.setNonReplicableProperty(ThrottleConstants.THROTTLE_INFO_KEY,
                            throttleDataHolder);
                }
            }
        }
    }
}
