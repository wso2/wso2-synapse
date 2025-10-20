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

package org.apache.synapse.startup.quartz;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.config.xml.FactoryUtils;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.config.xml.StartupFactory;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseException;
import org.apache.synapse.task.TaskDescription;
import org.apache.synapse.task.TaskDescriptionFactory;

import java.util.Properties;

/**
 * &lt;task class="org.my.synapse.Task" name="string"&gt;
 *  &lt;description&gt;description in text&lt;/description&gt;
 *  &lt;property name="stringProp" value="String"/&gt;
 *  &lt;property name="xmlProp"&gt;
 *   &lt;somexml&gt;config&lt;/somexml&gt;
 *  &lt;/property&gt;
 *  &lt;trigger ([[count="10"]? interval="1000"] | [cron="0 * 1 * * ?"] | [once=(true | false)])/&gt;
 * &lt;/task&gt;
 */
public class SimpleQuartzFactory implements StartupFactory {

    public final static QName TASK
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "task");

    private final static QName NAME = new QName("name");
    private final static QName VALUE = new QName("value");

    private final static Log log = LogFactory.getLog(SimpleQuartzFactory.class);

    public Startup createStartup(OMElement el) {
        
        return createSimpleQuartzStartup(el, null);
    }

    @Override
    public Startup createStartup(OMElement elem, Properties properties) {

        return createSimpleQuartzStartup(elem, properties);
    }

    private Startup createSimpleQuartzStartup(OMElement el, Properties properties) {

        if (log.isDebugEnabled()) {
            log.debug("Creating StartUpController Task");
        }
        
        if (el.getQName().equals(TASK)) {
            
            StartUpController startUpController = new StartUpController();
            TaskDescription taskDescription =
                    TaskDescriptionFactory.createTaskDescription(el,
                            XMLConfigConstants.SYNAPSE_OMNAMESPACE);
            if (taskDescription == null) {
                handleException("Invalid task - Task description can not be created  from :" + el);
                return null;
            }
            if (FactoryUtils.isVersionedDeployment(properties)) {
                updateTaskForVersioning(taskDescription, properties);
            }
            startUpController.setName(taskDescription.getName());
            startUpController.setTaskDescription(taskDescription);
            startUpController.setDescription(taskDescription.getTaskDescription());
            String nameString = startUpController.getName();
            if (nameString == null || "".equals(nameString)) {
                nameString = SynapseConstants.TASK_NAME;
            }
            AspectConfiguration aspectConfiguration = new AspectConfiguration(nameString);
            startUpController.configure(aspectConfiguration);

            OMAttribute statistics =
                    el.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE
                            , XMLConfigConstants.STATISTICS_ATTRIB_NAME));
            if (statistics != null) {
                String statisticsValue = statistics.getAttributeValue();
                if (statisticsValue != null) {
                    if (XMLConfigConstants.STATISTICS_ENABLE.equals(statisticsValue)) {
                        aspectConfiguration.enableStatistics();
                    }
                }
            }

            OMAttribute tracing = el.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE,
                    XMLConfigConstants.TRACE_ATTRIB_NAME));
            if (tracing != null) {
                String tracingValue = tracing.getAttributeValue();
                if (tracingValue != null) {
                    if (XMLConfigConstants.TRACE_ENABLE.equals(tracingValue)) {
                        aspectConfiguration.enableTracing();
                    }
                }
            }
            return startUpController;
        } else {
            handleException("Syntax error in the task : wrong QName for the task");
            return null;
        }
    }

    /**
     * This method will update the sequence name or proxy name in the task description for versioned deployment.
     *
     * @param taskDescription The task description of the Task
     * @param properties      Properties containing the artifact identifier and dependencies
     */
    private void updateTaskForVersioning(TaskDescription taskDescription, Properties properties) {

        taskDescription.setName(FactoryUtils.getFullyQualifiedName(properties, taskDescription.getName()));
        taskDescription.getXmlProperties().forEach(prop -> {
            if (prop.getAttributeValue(NAME).equals("sequenceName")) {
                prop.setText(FactoryUtils.getFullyQualifiedName(properties, prop.getText()));
                String value = prop.getAttributeValue(VALUE);
                if (value != null) {
                    String updatedValue = FactoryUtils.getFullyQualifiedName(properties, value);
                    prop.getAttribute(VALUE).setAttributeValue(updatedValue);
                }
            }
        });
    }

    public Class<SimpleQuartzSerializer> getSerializerClass() {
        return SimpleQuartzSerializer.class;
    }

    public QName getTagQName() {
        return TASK;
    }  

    private void handleException(String message) {
        log.error(message);
        throw new SynapseException(message);
    }

}
