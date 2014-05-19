package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.task.TaskManager;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Properties;


public class TaskManagerFactory {

    private static final Log log = LogFactory.getLog(TaskManagerFactory.class);

    public static final QName PROVIDER_Q
            = new QName(XMLConfigConstants.NULL_NAMESPACE, "provider");
    public static final QName PARAMETER_Q
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "parameter");
    public static final QName NAME_Q
            = new QName(XMLConfigConstants.NULL_NAMESPACE, "name");

    public static TaskManager createTaskManager(OMElement elem, Properties properties) {

        OMAttribute prov = elem.getAttribute(PROVIDER_Q);
        if (prov != null) {
            try {
                Class provider = Class.forName(prov.getAttributeValue());
                TaskManager taskManager = (TaskManager) provider.newInstance();
                taskManager.init(getProperties(elem, properties));
                taskManager.setConfigurationProperties(getProperties(elem, properties));
                return taskManager;

            } catch (ClassNotFoundException e) {
                handleException("Cannot locate task provider class : " +
                        prov.getAttributeValue(), e);
            } catch (IllegalAccessException e) {
                handleException("Error instantiating task provider : " +
                        prov.getAttributeValue(), e);
            } catch (InstantiationException e) {
                handleException("Error instantiating task provider : " +
                        prov.getAttributeValue(), e);
            }
        } else {
            handleException("The task 'provider' " +
                    "attribute is required for a taskManager definition");
        }

        return null;
    }

    private static Properties getProperties(OMElement elem, Properties topLevelProps) {
        Iterator params = elem.getChildrenWithName(PARAMETER_Q);
        Properties props = new Properties(topLevelProps);
        while (params.hasNext()) {
            Object o = params.next();
            if (o instanceof OMElement) {
                OMElement prop = (OMElement) o;
                OMAttribute pname = prop.getAttribute(NAME_Q);
                String propertyValue = prop.getText();
                if (pname != null) {
                    if (propertyValue != null) {
                        props.setProperty(pname.getAttributeValue(), propertyValue.trim());
                    }
                } else {
                    handleException("Invalid taskManager property - property should have a name ");
                }
            } else {
                handleException("Invalid taskManager property");
            }
        }
        return props;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
