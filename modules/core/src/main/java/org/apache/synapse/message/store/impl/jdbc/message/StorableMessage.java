/**
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.message.store.impl.jdbc.message;

import java.io.Serializable;

/**
 * This represents the final JDBC message that will be saved in the JDBC database.
 */
public class StorableMessage implements Serializable {
    private JDBCAxis2Message axis2Message;
    private JDBCSynapseMessage synapseMessage;

    public JDBCAxis2Message getAxis2Message() {
        return axis2Message;
    }

    public void setAxis2Message(JDBCAxis2Message axis2Message) {
        this.axis2Message = axis2Message;
    }

    public JDBCSynapseMessage getSynapseMessage() {
        return synapseMessage;
    }

    public void setSynapseMessage(JDBCSynapseMessage synapseMessage) {
        this.synapseMessage = synapseMessage;
    }
}