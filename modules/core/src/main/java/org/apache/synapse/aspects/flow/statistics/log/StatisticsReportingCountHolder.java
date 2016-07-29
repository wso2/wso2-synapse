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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is to hold stat count information(stat count, callback count) inside event holder.
 */
public class StatisticsReportingCountHolder {
    /**
     * Which holds count of open events.
     */
    private AtomicInteger statCount = new AtomicInteger(0);

    /**
     * Which holds count of call backs registered.
     */
    private AtomicInteger callBackCount = new AtomicInteger(0);

    public void incrementStatCount() {
        this.statCount.incrementAndGet();
    }

    public int decrementAndGetStatCount() {
        return this.statCount.decrementAndGet();
    }

    public int getStatCount() {
        return this.statCount.get();
    }

    public void incrementCallBackCount() {
        this.callBackCount.incrementAndGet();
    }

    public int decrementAndGetCallbackCount() {
        return this.callBackCount.decrementAndGet();
    }

    public int getCallBackCount() {
        return this.callBackCount.get();
    }

}
