/**
 *  Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.synapse.message.processor.impl;

/**
 * Defines all the possible states for the Message Processor.
 * 
 */
public enum MessageProcessorState {
	/**
	 * This is used to represent an undefined state which we are NOT interested
	 * in. For an example before starting the message processor, we assume that
	 * it is in this undefined state.
	 */
	OTHER,
	/**
	 * The message processor is started/activated.
	 */
	STARTED,
	/**
	 * The message processor is paused.
	 */
	PAUSED,
	/**
	 * The message processor is deactivated.
	 */
	STOPPED,
	/**
	 * The message processor is destroyed/deleted.
	 */
	DESTROYED;

}
