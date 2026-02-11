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
package org.apache.synapse.aspects;

import org.apache.synapse.Identifiable;
import org.apache.synapse.aspects.statistics.StatisticsConfigurable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aspect configuration
 * Currently contains only statistics configuration related things
 */
public class AspectConfiguration implements StatisticsConfigurable, Identifiable {

	/* Whether statistics enable */
	private boolean statisticsEnable = false;

	/* Whether tracing (collecting payload + message context properties) enabled */
	private boolean tracingEnabled = false;

	/* Identifier for a particular aspects configuration */
	private String id;

	private String uniqueId;

	private Integer hashCode = null;

    private boolean traceFilterEnable = false;

    public AspectConfiguration(String id) {
		this.id = id;
	}

	public boolean isStatisticsEnable() {
		return statisticsEnable;
	}

	public void disableStatistics() {
		if (statisticsEnable) {
			this.statisticsEnable = false;
		}
	}

	public void enableStatistics() {
		if (!statisticsEnable) {
			statisticsEnable = true;
		}
	}

	public boolean isTracingEnabled() {
		return tracingEnabled;
	}

	public void disableTracing() {
		if (tracingEnabled) {
			this.tracingEnabled = false;
		}
	}

	public void enableTracing() {
		if (!tracingEnabled) {
			this.tracingEnabled = true;
		}
	}

	public String getId() {
		return id;
	}

	public void setStatisticsEnable(boolean statisticsEnable) {
		this.statisticsEnable = statisticsEnable;
	}

	public void setTracingEnabled(boolean tracingEnabled) {
		this.tracingEnabled = tracingEnabled;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		if (this.uniqueId == null) {
			this.uniqueId = uniqueId;
		}
	}

	public void setHashCode(String hashCode) {
		if (this.hashCode == null) {
			this.hashCode = Integer.parseInt(hashCode);
//			this.hashCode = hashCode.hashCode();
		}
	}

	public Integer getHashCode() {
		return hashCode;
	}

    public void enableTraceFilter() {
        this.traceFilterEnable = true;
    }

    public void disableTraceFilter() {
        this.traceFilterEnable = false;
    }

    public boolean isTraceFilterEnable() {
        return traceFilterEnable;
    }
}
