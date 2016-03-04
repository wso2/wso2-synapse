/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.flow.statistics;

import org.apache.synapse.aspects.ComponentType;

public class StatisticIdentityGenerator {

	private static int id = 0;

	private static int hashCode = 0;

	private static String parent;

	public static String getIdString() {
		return String.valueOf(id++);
	}

	public static void resetId() {
		//System.out.println(">>>>>>>HASHCODE for: " + hashCode);
		id = 0;
		hashCode = 0;
	}

	public static void setParent(String parentName) {
		parent = parentName + "@";
	}

	public static String getIdForComponent(String name, ComponentType componentType) {
		String id = parent + getIdString() + ":" + name;
		hashCode += id.hashCode();
		System.out.println(id);
		return id;
	}

	public static String getIdReferencingComponent(String name, ComponentType componentType) {
		String idString = name + "@0:" + name;
		id++;
		hashCode += idString.hashCode();
		System.out.println(idString);
		return idString;
	}

	public static void reportingBranchingEvents() {
		System.out.println("Branching Happening, IF~else // Clone Targets");
	}

	public static void reportingEndEvent(String mediatorId, ComponentType componentType) {
		System.out.println("Ending Component Initialization:" + mediatorId);
	}

	public static String getHashCode() {
		System.out.println("Hash Code Given to the component is :" + hashCode);
		return String.valueOf(hashCode);
	}

	public static String getIdForFlowContinuableMediator(String mediatorName, ComponentType mediator) {
		String id = parent + getIdString() + ":" + mediatorName;
		hashCode += id.hashCode();
		System.out.println(id);
		return id;
	}

	public static void reportingFlowContinuableEndEvent(String mediatorId, ComponentType mediator) {
		System.out.println("Ending Flow Continuable Component Initialization:" + mediatorId);
	}

	public static void reportingEndBranchingEvent() {
		System.out.println("Branching Ended, IF~else // Clone Targets");
	}
}
