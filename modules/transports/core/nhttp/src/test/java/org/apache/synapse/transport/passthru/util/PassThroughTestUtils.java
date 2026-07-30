/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.transport.passthru.util;

import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPoolFactory;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;

import java.io.File;

/**
 * Provides utility methods for PassTrough Transport tests.
 */
public class PassThroughTestUtils {

    private static final String PASSTHROUGH_THREAD_GROUP = "Pass-through Message Processing Thread Group";
    private static final String PASSTHROUGH_THREAD_ID = "PassThroughMessageProcessor";
    private static final String PASSTHROUGH_RESOURCE_PATH = File.separator + "src" + File.separator + "test" +
            File.separator + "resources" + File.separator + "org.apache.synapse.transport.ptt.conf" + File.separator;

    /**
     * Creates a PassThroughConfiguration instance with provided passthru-http.properties
     *
     * @return PassThroughConfiguration instance
     */
    public static PassThroughConfiguration getPassThroughConfiguration() {
        String testConfLocation = System.getProperty("user.dir") + PASSTHROUGH_RESOURCE_PATH;
        System.setProperty(PassThroughConstants.CONF_LOCATION, testConfLocation);
        return PassThroughConfiguration.getInstance();
    }

    /**
     * Creates a workerpool from Passthrough configurations
     *
     * @param conf Passthrough Configuration
     * @return Worker Pool
     */
    public static WorkerPool getWorkerPool(PassThroughConfiguration conf) {
        return WorkerPoolFactory.getWorkerPool(conf.getWorkerPoolCoreSize(), conf.getWorkerPoolMaxSize(),
                conf.getWorkerThreadKeepaliveSec(), conf.getWorkerPoolQueueLen(),
                PASSTHROUGH_THREAD_GROUP, PASSTHROUGH_THREAD_ID);
    }

}
