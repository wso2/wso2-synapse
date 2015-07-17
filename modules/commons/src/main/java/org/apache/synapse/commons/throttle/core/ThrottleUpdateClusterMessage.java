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

import java.util.List;


/**
 * Used to Replicate CallerContexts through the Cluster. A Cluster message receives a bundle of
 * CallerContexts, changed until a particular point. Upon receiving the message, all the
 * CallerContexts present in the list are iterated and updated.
 */

public class ThrottleUpdateClusterMessage extends ClusteringMessage {
    private static final Log log = LogFactory.getLog(ThrottleUpdateClusterMessage.class);

    private List<String> keys;
    private List<CallerContext> callerContexts;


    public ThrottleUpdateClusterMessage(List<String> keys, List<CallerContext> callerContexts) {
        this.keys = keys;
        this.callerContexts = callerContexts;
        log.debug("Initializing with " + callerContexts.size()
                                                    + " CallerContexts " + this.getUuid());
    }

    @Override
    public ClusteringCommand getResponse() {
        return null;
    }

    @Override
    public void execute(ConfigurationContext configContext) throws ClusteringFault {
        if (log.isDebugEnabled()) {
            log.debug("Received ThrottleUpdateClusterMessage " + this.getUuid());
        }

        ThrottleDataHolder throttleDataHolder = (ThrottleDataHolder)
                configContext.getProperty(ThrottleConstants.THROTTLE_INFO_KEY);
        if (throttleDataHolder != null) {
            if (keys != null && !keys.isEmpty() && callerContexts != null &&
                    !callerContexts.isEmpty()) {
                int i = 0;
                log.debug("Received ThrottleUpdateClusterMessage with key size : "
                        + keys.size() + " UUID : " + this.getUuid());
                log.trace("Start executing ClusterMessage : " + this.getUuid());
                for (String key : keys) {
                    CallerContext localCallerContext = throttleDataHolder.getCallerContext(key);
                    log.debug("Getting CallerContext for key : " + key);
                    CallerContext replicatedCallerContext = callerContexts.get(i++);
                    log.debug("Replicated CallerContext for key " + key + " , "
                            + replicatedCallerContext.getID());
                    if (localCallerContext == null) {
                        synchronized (key.intern()) {
                            localCallerContext = throttleDataHolder.getCallerContext(key);
                            if (localCallerContext == null) {
                                log.debug("Cannot find local Context");
                                replicatedCallerContext.resetLocalCounter();
                                log.debug("Reset localCounter");
                                throttleDataHolder
                                        .addCallerContextIfAbsent(key, replicatedCallerContext);
                                log.debug("Added the Replicated Context.");
                            }
                        }
                    } else {

                        if (replicatedCallerContext.getGlobalCounter() -
                                localCallerContext.getGlobalCounter() > 100) {
                            localCallerContext.setGlobalCounter(
                                    replicatedCallerContext.getGlobalCounter() +
                                            localCallerContext.getLocalCounter());
                            log.debug("Difference More than 100 " +
                                    " Replicated Context Counter : " +
                                    replicatedCallerContext.getGlobalCounter() +
                                    " Local Context Global : " +
                                    localCallerContext.getGlobalCounter() +
                                    "Local Context Local : " +
                                    localCallerContext.getLocalCounter());
                        } else {
                            localCallerContext.
                               incrementGlobalCounter(replicatedCallerContext.getLocalCounter());
                            log.debug("Difference Less : Replicated Local : " +
                                    replicatedCallerContext.getLocalCounter() +
                                    " Global : " + localCallerContext.getGlobalCounter());
                        }
                    }
                }
                log.trace("Finished executing ClusterMessage : " + this.getUuid());
            }
        }
    }

}

