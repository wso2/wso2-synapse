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

package org.apache.synapse.startup.tasks;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.xml.ValueFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.template.InvokeMediator;
import org.apache.synapse.task.Task;

import java.util.Iterator;

public class TemplateMessageExecutor implements Task, ManagedLifecycle {

    private static final String TEMPLATE_SEQUENCE = "_Template_Sequence";
    private SynapseEnvironment synapseEnvironment;
    private SequenceMediator seqMed;
    private String templateKey;
    private OMElement templateParams;
    private InvokeMediator invoker;

    public void init(SynapseEnvironment se) {
        //Initialize the template and populate the parameters
        synapseEnvironment = se;
        invoker = new InvokeMediator();
        invoker.setTargetTemplate(templateKey);
        buildParameters(templateParams);
        // Remove if there's a sequence already exists
        if (se.getSynapseConfiguration().getSequence(TEMPLATE_SEQUENCE + "_" + templateKey.hashCode()) != null) {
            se.getSynapseConfiguration().removeSequence(TEMPLATE_SEQUENCE + "_" + templateKey.hashCode());
        }
        seqMed = new SequenceMediator();
        seqMed.setName(TEMPLATE_SEQUENCE + "_" + templateKey.hashCode());
        seqMed.addChild(invoker);
        se.getSynapseConfiguration().addSequence(seqMed.getName(), seqMed);
    }

    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void execute() {
        //Inject the message into SynapseEnvironment
        MessageContext mc = synapseEnvironment.createMessageContext();
        mc.pushFaultHandler(new MediatorFaultHandler(mc.getFaultSequence()));
        synapseEnvironment.injectAsync(mc, seqMed);
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public void setTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }


    public OMElement getTemplateParams() {
        return templateParams;
    }

    public void setTemplateParams(OMElement templateParams) {
        this.templateParams = templateParams;
    }

    private void buildParameters(OMElement elem) {
        Iterator subElements = elem.getChildElements();
        while (subElements.hasNext()) {
            OMElement child = (OMElement) subElements.next();
            Value paramValue = new ValueFactory().createTextValue(child);
            invoker.addExpressionForParamName(child.getLocalName(), paramValue);
        }
    }
}
