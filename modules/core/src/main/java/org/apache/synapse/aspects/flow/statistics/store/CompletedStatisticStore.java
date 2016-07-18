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

package org.apache.synapse.aspects.flow.statistics.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class will hold completed statistic entries till they are collected for by carbon mediation.
 */
public class CompletedStatisticStore {
    private static Log log = LogFactory.getLog(CompletedStatisticStore.class);


	/**
	 * Completed statistics entries for message flows.
	 */
	private final List<PublishingFlow> completedStatisticEntries;

	public CompletedStatisticStore() {
		completedStatisticEntries = new LinkedList<>();

	}

	public void putCompletedStatisticEntry(PublishingFlow publishingFlow) {
		synchronized (completedStatisticEntries) {
            logEvent(publishingFlow);
			completedStatisticEntries.add(publishingFlow);
		}
	}


    private static void logEvent(PublishingFlow publishingFlow) {
        Map<String, Object> mapping = publishingFlow.getObjectAsMap();
        mapping.put("host", "localhost"); // Adding host
        mapping.put("tenantId", "1234");

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = null;
        try {
            jsonString = mapper.writeValueAsString(mapping);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert", e);
        }
        log.info("Uncompressed data ********************* :" + jsonString);
    }

	public boolean isEmpty() {
		return completedStatisticEntries.isEmpty();
	}

	public List<PublishingFlow> getCompletedStatisticEntries() {
		List<PublishingFlow> cloneOfCompletedStatisticEntries = new LinkedList<>();
		synchronized (completedStatisticEntries) {
			cloneOfCompletedStatisticEntries.addAll(completedStatisticEntries);
			completedStatisticEntries.clear();
		}
		return cloneOfCompletedStatisticEntries;
	}
}
