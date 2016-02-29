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

package org.apache.synapse.aspects.flow.statistics.data.raw;

public class CallbackDataUnit extends BasicStatisticDataUnit {
	private String callbackId;
	private Boolean isContinuationCall;
	private boolean isOutOnlyFlow;

	public String getCallbackId() {
		return callbackId;
	}

	public void setCallbackId(String callbackId) {
		this.callbackId = callbackId;
	}

	public Boolean getIsContinuationCall() {
		return isContinuationCall;
	}

	public void setIsContinuationCall(Boolean isContinuationCall) {
		this.isContinuationCall = isContinuationCall;
	}

	public boolean isOutOnlyFlow() {
		return isOutOnlyFlow;
	}

	public void setIsOutOnlyFlow(boolean isOutOnlyFlow) {
		this.isOutOnlyFlow = isOutOnlyFlow;
	}
}
