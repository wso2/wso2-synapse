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

package org.apache.synapse.config.xml.eventing;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.AbstractMediatorSerializer;
import org.apache.synapse.mediators.eventing.EventPublisherMediator;

/**
 * Serializer for {@link org.apache.synapse.mediators.eventing.EventPublisherMediator} instances.
 */
public class EventPublisherMediatorSerializer extends AbstractMediatorSerializer {

    public OMElement serializeSpecificMediator(Mediator m) {

        if (!(m instanceof EventPublisherMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        EventPublisherMediator mediator = (EventPublisherMediator) m;
        OMElement elmEventPublisher = fac.createOMElement("eventPublisher", synNS);

        if (mediator.getEventSourceName() != null) {
            elmEventPublisher.addAttribute(fac.createOMAttribute(
                    "eventSourceName", nullNS, mediator.getEventSourceName()));
        } else {
            handleException("Invalid EventPublisher mediator. Event Source Name required");
        }
        saveTracingState(elmEventPublisher, mediator);

        serializeComments(elmEventPublisher, mediator.getCommentsList());

        return elmEventPublisher;
    }

    public String getMediatorClassName() {
        return EventPublisherMediator.class.getName();
    }
}
