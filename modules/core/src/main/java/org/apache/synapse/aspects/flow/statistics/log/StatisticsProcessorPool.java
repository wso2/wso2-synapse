/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.aspects.flow.statistics.log;

import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPoolFactory;

/**
 * Created by rajith on 7/19/16.
 */
public class StatisticsProcessorPool {
    /** the thread pool to execute actual statistics processing invocations */
    protected WorkerPool workerPool = null;

    private static StatisticsProcessorPool eventProcessor;

    private StatisticsProcessorPool() {
        if (this.workerPool == null) {
//            this.workerPool = WorkerPoolFactory.getWorkerPool( todo implement to get from config
//                    config.getServerCoreThreads(),
//                    config.getServerMaxThreads(),
//                    config.getServerKeepalive(),
//                    config.getServerQueueLen(),
//                    getTransportName() + "Server Worker thread group",
//                    getTransportName() + "-Worker");
            this.workerPool = WorkerPoolFactory.getWorkerPool(
                    400,
                    500,
                    60,
                    100,
                    "Statistics processor Thread group",
                    "Statistics-Worker");
        }
    }

    public static StatisticsProcessorPool getInstance() {
        if (eventProcessor == null) {
            synchronized ("dfd") {
                if (eventProcessor == null) {
                    eventProcessor = new StatisticsProcessorPool();
                }
            }
        }
        return eventProcessor;
    }


    public void execute(Runnable executor) {
        this.workerPool.execute(executor);
    }
}
