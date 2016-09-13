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

package org.apache.synapse.mediators.transform;

import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;

public class Argument {

    private String value;
    private SynapsePath expression;
    private boolean deepCheck = true;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public SynapsePath getExpression() {
        return expression;
    }

    public void setExpression(SynapsePath expression) {
        this.expression = expression;
    }

    public boolean isDeepCheck() {
        return deepCheck;
    }

    public void setDeepCheck(boolean deepCheck) {
        this.deepCheck = deepCheck;
    }
}
