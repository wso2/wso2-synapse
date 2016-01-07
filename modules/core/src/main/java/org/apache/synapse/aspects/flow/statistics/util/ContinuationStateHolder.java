/*
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.flow.statistics.util;

/**
 * This class is used hold statistic data in continuation calls. It will have reference to callback related to the
 * continuation and state of continuation stack.
 */
public class ContinuationStateHolder {

	private int callbackPoint;

	private int continuationStackPosition;

	public ContinuationStateHolder(int callbackPoint, int searchIndexLimit) {
		this.callbackPoint = callbackPoint;
		this.continuationStackPosition = searchIndexLimit;
	}

	public int getCallbackPoint() {
		return callbackPoint;
	}

	public int getContinuationStackPosition() {
		return continuationStackPosition;
	}

	public void setContinuationStackPosition(int continuationStackPosition) {
		this.continuationStackPosition = continuationStackPosition;
	}
}
