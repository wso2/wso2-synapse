package org.apache.synapse.config.xml;

import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.builtin.ForEachMediator;
import org.apache.synapse.mediators.eip.splitter.IterateMediator;
import org.jaxen.JaxenException;

/**
 * <foreach expression> </foreach>
 */
public class ForEachMediatorFactory extends AbstractMediatorFactory {

	private static final Log log = LogFactory.getLog(ForEachMediatorFactory.class);

	private static final QName FOREACH_Q = new QName(SynapseConstants.SYNAPSE_NAMESPACE, "foreach");
	private static final QName ATT_EXPRESSION = new QName("expression");

	// static final QName DEFAULT_PERCENTAGE_Q = new QName(
	// XMLConfigConstants.SYNAPSE_NAMESPACE, "discount");

	public QName getTagQName() {
		return FOREACH_Q;
	}

	@Override
	protected Mediator createSpecificMediator(OMElement elem, Properties arg1) {
		ForEachMediator mediator = new ForEachMediator();
        processAuditStatus(mediator, elem);
        OMAttribute expression = elem.getAttribute(ATT_EXPRN);
        if (expression != null) {
            try {
                mediator.setExpression(SynapseXPathFactory.getSynapseXPath(elem, ATT_EXPRN));
            } catch (JaxenException e) {
                handleException("Unable to build the ForEachMediator. " + "Invalid XPATH " +
                    expression.getAttributeValue(), e);
            }
        } else {
            handleException("XPATH expression is required " +
                "for an ForEach under the \"expression\" attribute");
        }
        return mediator;

	}

}
