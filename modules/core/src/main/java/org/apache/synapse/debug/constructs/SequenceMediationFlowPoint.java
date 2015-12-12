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

/**
 * Defines a unique point in the mediation route that mediate through a Sequence
 */
public class SequenceMediationFlowPoint extends SynapseMediationFlowPoint {
    /*defined Synapse sequence type*/
    private SynapseSequenceType seqType;
    /*either inbound,sequence, api or proxy*/
    private String sequenceBaseType;

    public String getSequenceBaseType() {
        return sequenceBaseType;
    }

    public void setSequenceBaseType(String sequenceBaseType) {
        this.sequenceBaseType = sequenceBaseType;
    }

    public void setSynapseSequenceType(SynapseSequenceType seqType) {
        this.seqType = seqType;
    }

    public SynapseSequenceType getSynapseSequenceType() {
        return seqType;
    }

}
