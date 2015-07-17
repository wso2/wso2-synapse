/*
*  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.commons.throttle.core;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConcurrentAccessReplicator {

    private static final Log log = LogFactory.getLog(ConcurrentAccessReplicator.class);
    private ConfigurationContext configContext;

    public ConcurrentAccessReplicator(ConfigurationContext configContext) {
        this.configContext = configContext;
    }

    public void replicate(String key, ConcurrentAccessController concurrentAccessController) {
        try {
            ClusteringAgent clusteringAgent =
                    configContext.getAxisConfiguration().getClusteringAgent();
            if (clusteringAgent != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Replicating ConcurrentAccessController " + key +
                            " in a ClusterMessage.");
                }
                clusteringAgent.sendMessage(
                        new ConcurrentAccessUpdateClusterMessage(key, concurrentAccessController),
                        true);
            }
        } catch (ClusteringFault e) {
            if (log.isErrorEnabled()) {
                log.error("Could not replicate throttle data", e);
            }
        }
    }
}
