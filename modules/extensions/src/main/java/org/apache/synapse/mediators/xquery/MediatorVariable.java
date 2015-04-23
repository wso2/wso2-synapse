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
package org.apache.synapse.mediators.xquery;

import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.XdmNodeKind;
import org.apache.synapse.MessageContext;

import javax.xml.namespace.QName;

/**
 * Base class representing mediator variable used in XQuery mediator
 */

public abstract class MediatorVariable {

    private QName name;
    private ItemType type;
    private XdmNodeKind nodeKind;
    protected Object value;


    public MediatorVariable(QName name) {
        this.name = name;
    }

    public QName getName() {
        return name;
    }

    public void setName(QName name) {
        this.name = name;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public XdmNodeKind getNodeKind() {
        return nodeKind;
    }

    public void setNodeKind(XdmNodeKind nodeKind) {
        this.nodeKind = nodeKind;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Calculates the value of this variable
     *
     * @param synCtx Current message in transit
     * @return <code>true</code> if the value has changed
     */
    public abstract boolean evaluateValue(MessageContext synCtx);

}
