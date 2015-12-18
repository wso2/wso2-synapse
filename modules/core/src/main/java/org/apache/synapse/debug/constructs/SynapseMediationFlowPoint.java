/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.debug.constructs;

import org.apache.synapse.Mediator;

/**
 * Defines a unique point in the mediation flow, when message flows through the mediation engine.
 */
public class SynapseMediationFlowPoint {
    /*defines the mediation component either template, sequence or connector*/
    private SynapseMediationComponent medComponent = null;
    /*reference to the mediator where registered breakpoint belongs to*/
    private Mediator medRef = null;
    /*key of the mediation component */
    private String key = null;
    /*mediator position with related to parent mediator*/
    private int[] mediatorPosition = null;


    public SynapseMediationComponent getSynapseMediationComponent() {
        return medComponent;
    }

    public void setSynapseMediationComponent(SynapseMediationComponent medComponent) {
        this.medComponent = medComponent;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setMediatorPosition(int[] mediatorPosition) {
        this.mediatorPosition = mediatorPosition;
    }

    public int[] getMediatorPosition() {
        return mediatorPosition;
    }

    public void setMediatorReference(Mediator medRef) {
        this.medRef = medRef;
    }

    public Mediator getMediatorReference() {
        return medRef;
    }

    public String toString() {
        return "";
    }

}
