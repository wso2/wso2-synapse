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
 * Synapse Sequence types
 */
public enum SynapseSequenceType {

    NAMED {
        public String toString() {
            return "NAMED";
        }
    },
    PROXY_INSEQ {
        public String toString() {
            return "PROXY_INSEQ";
        }
    },
    PROXY_OUTSEQ {
        public String toString() {
            return "PROXY_OUTSEQ";
        }
    },
    PROXY_FAULTSEQ {
        public String toString() {
            return "PROXY_FAULTSEQ";
        }
    },
    API_INSEQ {
        public String toString() {
            return "API_INSEQ";
        }
    },
    API_OUTSEQ {
        public String toString() {
            return "API_OUTSEQ";
        }
    },
    API_FAULTSEQ {
        public String toString() {
            return "API_FAULTSEQ";
        }
    },
    INBOUND_SEQ {
        public String toString() {
            return "INBOUND_SEQ";
        }
    },
    INBOUND_FAULTSEQ {
        public String toString() {
            return "INBOUND_FAULTSEQ";
        }
    }

}
