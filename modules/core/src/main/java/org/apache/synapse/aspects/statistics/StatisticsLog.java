/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.aspects.statistics;

import org.apache.synapse.aspects.ComponentType;

/**
 * Statistics at a break point
 */
public class StatisticsLog {

    private String id;

    private ComponentType componentType;

    private long time;

    private boolean isResponse = false;

    private boolean isFault = false;

    private boolean isEndAnyLog = false;

    private ErrorLog errorLog;
    /*
	 * A flag which tells whether the statistics log is collected by the request
	 * flow or not. This is important when we have clone/aggregate mediators.
	 * This prevents the same statistic log being collected by both the request
	 * and response flows.
	 */
	private boolean collectedByRequestFlow = false;

    public StatisticsLog(String id, ComponentType componentType) {
        this(id, System.currentTimeMillis(), componentType);
    }

    public StatisticsLog(String id, long startTime, ComponentType componentType) {
        this.id = id;
        this.time = startTime;
        this.componentType = componentType;
    }

    public String getId() {
        return id;
    }

    public ComponentType getComponentType() {
        return componentType;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isResponse() {
        return isResponse;
    }

    public boolean isFault() {
        return isFault;
    }

    public void setFault(boolean fault) {
        isFault = fault;
    }

    public void setResponse(boolean response) {
        isResponse = response;
    }

    public void setEndAnyLog(boolean endAnyLog) {
        isEndAnyLog = endAnyLog;
    }

    public boolean isEndAnyLog() {
        return isEndAnyLog;
    }

    public ErrorLog getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(ErrorLog errorLog) {
        this.errorLog = errorLog;
    }

    @Override
    public String toString() {
        return "StatisticsLog{" +
                "id='" + id + '\'' +
                ", componentType=" + componentType +
                '}';
    }
    
	/**
	 * Tells whether this statistic log is collected via the request flow or
	 * not.
	 * 
	 * @return <code>true</code> if the statistics log is collected by the
	 *         request flow, <code>false</code> otherwise
	 */
	public boolean isCollectedByRequestFlow() {
		return collectedByRequestFlow;
	}

	/**
	 * Flags whether this statistics log is collected by the request flow or
	 * not.
	 * 
	 * @param collectedByProxy
	 *            <code>true</code> if this statistic log is collected by the
	 *            request flow, <code>false</code> otherwise
	 */
	public void setCollectedByRequestFlow(boolean collectedByRequestFlow) {
		this.collectedByRequestFlow = collectedByRequestFlow;
	}
}
