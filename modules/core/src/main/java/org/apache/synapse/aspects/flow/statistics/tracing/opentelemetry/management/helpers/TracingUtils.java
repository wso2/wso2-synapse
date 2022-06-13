/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.helpers;

import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

/**
 * Contains utility methods related to OpenTracing.
 */
public class TracingUtils {

    /**
     * Prevents Instantiation.
     */
    private TracingUtils() {}

    /**
     * Extracts the explicit id which is directly used to start and end spans, from the provided basic statistic data
     * unit.
     * @param basicStatisticDataUnit    Basic statistic data unit which explicitly contains the id.
     * @return                          Extracted id.
     */
    public static String extractId(BasicStatisticDataUnit basicStatisticDataUnit) {
        return String.valueOf(basicStatisticDataUnit.getCurrentIndex());
    }

    /**
     * Returns the same hash code for the given object as would be returned by the default method hashCode(),
     * whether or not the given object's class overrides hashCode().
     * The hash code for the null reference is zero.
     * @param object    An object.
     * @return          System identity hash code for the given object.
     */
    public static String getSystemIdentityHashCode(Object object) {
        return String.valueOf(System.identityHashCode(object));
    }

    /**
     * Returns whether the given statistic data unit is from an anonymous sequence.
     * @param statisticDataUnit Statistic data unit object.
     * @return                  Whether the given statistic data unit is from an anonymous sequence.
     */
    public static boolean isAnonymousSequence(StatisticDataUnit statisticDataUnit) {
        return ComponentType.SEQUENCE.equals(statisticDataUnit.getComponentType()) &&
                "anonymoussequence".equalsIgnoreCase(statisticDataUnit.getComponentName());
    }
}
