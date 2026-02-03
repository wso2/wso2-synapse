/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse;

import org.apache.axis2.transport.base.AckDecision;
import org.apache.axis2.transport.base.AckDecisionCallback;
import org.apache.synapse.core.axis2.Axis2MessageContext;

/**
 * Utility to finalize the RabbitMQ ACK decision from mediation code.
 * <p>Usage: invoke from any mediator when you decide the outcome.
 *
 * <pre>
 *   AckDecisionSetter.set(synCtx, "ACKNOWLEDGE");
 *   // or
 *   AckDecisionSetter.set(synCtx, AckDecision.SET_REQUEUE_ON_ROLLBACK);
 * </pre>
 *
 * Contract:
 * - Reads Axis2 property RabbitMQConstants.ACKNOWLEDGEMENT_DECISION.
 *   If it's an AckDecisionCallback, completes it (first-writer wins).
 * - Always overwrites the same property with the final String for transparency/logging.
 */
public class AckDecisionSetter {

    private AckDecisionSetter() {}

    /**
     * Set the decision using string constants:
     * "ACKNOWLEDGE", "SET_ROLLBACK_ONLY", "SET_REQUEUE_ON_ROLLBACK".
     *
     * @param  axis2mc  message context
     * @param decisionStr decision string
     */
    public static void set(org.apache.axis2.context.MessageContext axis2mc, String decisionStr) {
        Object ackDecisionProperty = axis2mc.getProperty(SynapseConstants.ACKNOWLEDGEMENT_DECISION);

        // Convert string -> enum
        AckDecision decisionEnum = AckDecision.fromString(decisionStr);

        // If placeholder present, complete it (wakes waiting listener thread)
        if (ackDecisionProperty instanceof AckDecisionCallback) {
            ((AckDecisionCallback) ackDecisionProperty).complete(decisionEnum);
        }
    }

}
