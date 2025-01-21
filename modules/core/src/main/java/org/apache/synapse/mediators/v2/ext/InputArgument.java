/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.v2.ext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.mediators.v2.ScatterGatherUtils;

public class InputArgument {

    private static final Log log = LogFactory.getLog(InputArgument.class);
    private String name;
    private SynapsePath expression;
    private Object value;
    private String type;

    public InputArgument(String name) {

        this.name = name;
    }

    public Object getResolvedArgument(MessageContext synCtx) {

        return ScatterGatherUtils.getResolvedValue(synCtx, expression, value, type, log);
    }

    public void setExpression(SynapsePath expression, String type) {

        this.type = type;
        this.expression = expression;
    }

    public void setValue(String value, String type) {

        this.type = type;
        this.value = ScatterGatherUtils.convertValue(value, type, log);
    }

    public String getName() {

        return name;
    }

    public SynapsePath getExpression() {

        return expression;
    }

    public Object getValue() {

        return value;
    }

    public String getType() {

        return type;
    }
}
