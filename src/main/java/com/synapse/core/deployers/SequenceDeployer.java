/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package com.synapse.core.deployers;

import com.synapse.core.artifacts.Sequence;
import com.synapse.core.artifacts.Mediator;
import com.synapse.core.artifacts.utils.Position;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SequenceDeployer {

    public Sequence unmarshal(String xmlData, Position position) throws XMLStreamException {
        OMElement sequenceElement = AXIOMUtil.stringToOM(xmlData);

        if (sequenceElement == null || !sequenceElement.getLocalName().equals("sequence")) {
            return null;
        }
        OMAttribute nameAttr = sequenceElement.getAttribute(new javax.xml.namespace.QName("name"));
        if (nameAttr != null) {
                String sequenceName = nameAttr.getAttributeValue();

                Position newPosition = new Position(1, position.getFileName(), sequenceName);

                Sequence newSequence = unmarshalSequence(sequenceElement, newPosition);

                newSequence.setName(sequenceName);
                return newSequence;
        }
        return null;
    }
    private Sequence unmarshalSequence(OMElement sequenceElement, Position position) {
        List<Mediator> mediatorList = new ArrayList<>();

        for (Iterator it = sequenceElement.getChildrenWithName(new QName("log")); it.hasNext(); ) {
            OMElement mediatorElement = (OMElement) it.next();
            LogMediatorDeployer logMediatorDeployer = new LogMediatorDeployer();
            try {
                mediatorList.add(logMediatorDeployer.unmarshal(mediatorElement, position));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Sequence sequence = new Sequence();
        sequence.setMediatorList(mediatorList);
        sequence.setPosition(position);
        return sequence;
    }
}