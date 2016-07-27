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

/**
 * Created by rajith on 7/15/16.
 */
public class StatisticsReportingCountHolder {
    private int statCount = 0;

    private int callBackCount = 0;

    public void incrementStatCount() {
        this.statCount += 1;
    }

    public void decrementStatCount() {
        this.statCount -= 1;
    }

    public int getStatCount() {
        return this.statCount;
    }

    public void incrementCallBackCount() {
        this.callBackCount += 1;
    }

    public void decrementCallbackCount() {
        this.callBackCount -= 1;
    }

    public int getCallBackCount() {
        return this.callBackCount;
    }

}
