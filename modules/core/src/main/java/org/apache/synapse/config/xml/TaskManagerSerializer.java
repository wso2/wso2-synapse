package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.task.TaskManager;

import java.util.Iterator;


public class TaskManagerSerializer {

    private static final Log log = LogFactory.getLog(TaskManagerSerializer.class);

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS = SynapseConstants.SYNAPSE_OMNAMESPACE;
    protected static final OMNamespace nullNS = fac.createOMNamespace(XMLConfigConstants.NULL_NAMESPACE, "");

    public static OMElement serializetaskManager(OMElement parent, TaskManager taskManager) {

        OMElement executor = fac.createOMElement("taskManager", synNS);

        if (taskManager.getProviderClass() != null) {
            executor.addAttribute(fac.createOMAttribute(
                    "provider", nullNS, taskManager.getProviderClass()));
        } else {
            handleException("Invalid taskManager. Provider is required");
        }

        Iterator iter = taskManager.getConfigurationProperties().keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            String value = (String) taskManager.getConfigurationProperties().get(name);
            OMElement property = fac.createOMElement("parameter", synNS);
            property.addAttribute(fac.createOMAttribute(
                    "name", nullNS, name));
            property.setText(value.trim());
            executor.addChild(property);
        }

        if (parent != null) {
            parent.addChild(executor);
        }
        return executor;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
