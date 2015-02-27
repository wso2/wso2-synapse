/**
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.message.processor.impl;

import org.apache.synapse.task.TaskManager;
import org.wso2.carbon.mediation.ntask.NTaskTaskManager;

/**
 * This factory class is responsible for creating {@link TaskManager} instances.
 * This encapsulates all the <code>TaskManager</code> creation logics and
 * insulates the users from that.
 *
 */
public class TaskManagerFactory {
	private static TaskManager nTaskTaskManager = null;

	/*
	 * Explicitly defined private constructor.
	 */
	private TaskManagerFactory() {

	}

	/**
	 * Creates a singleton instance of {@link NTaskTaskManager} class and
	 * provides a global point of access to that.
	 * 
	 * @return A singleton instance of the {@link NTaskTaskManager} class.
	 */
	public static TaskManager createNTaskTaskManager() {
		if (nTaskTaskManager == null) {
			nTaskTaskManager = new NTaskTaskManager();
		}

		return nTaskTaskManager;
	}

}
