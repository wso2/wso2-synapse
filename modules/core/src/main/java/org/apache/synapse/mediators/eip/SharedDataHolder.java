/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.apache.synapse.mediators.eip;

/**
 * This class is used to hold the shared data for a particular message set
 * For an example we can use this to share some data across all the spawned spilt messages in iterate mediator
 */
public class SharedDataHolder {

    /**
     * Variable to track whether aggregation is completed.
     */
    private boolean isAggregationCompleted = false;

    /**
     * Check whether aggregation has been completed.
     *
     * @return whether completion has happened for aggregates
     */
    public boolean isAggregationCompleted() {
        return isAggregationCompleted;
    }

    /**
     * Mark completion for aggregates.
     */
    public void markAggregationCompletion() {
        isAggregationCompleted = true;
    }

}
