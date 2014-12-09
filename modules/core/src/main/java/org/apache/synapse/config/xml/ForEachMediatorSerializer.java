package org.apache.synapse.config.xml;


import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.ForEachMediator;

/**
 * <foreach> </foreach>
 */
public class ForEachMediatorSerializer extends AbstractMediatorSerializer {

	public String getMediatorClassName() {
		return ForEachMediator.class.getName();
	}

	@Override
	protected OMElement serializeSpecificMediator(Mediator m) {
		if (!(m instanceof ForEachMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }
        
        OMElement forEachElem = fac.createOMElement("foreach", synNS);
        saveTracingState(forEachElem, m);

        ForEachMediator forEachMed = (ForEachMediator) m;
       

        
        if (forEachMed.getExpression() != null) {
            SynapseXPathSerializer.serializeXPath(forEachMed.getExpression(), forEachElem, "expression");
        } else {
            handleException("Missing expression of the ForEach which is required.");
        }

        forEachElem.addChild(TargetSerializer.serializeTarget(forEachMed.getTarget()));

        return forEachElem;
	}
}
