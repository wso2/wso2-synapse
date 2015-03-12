/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.config.xml;

import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.CallMediator;
import org.apache.synapse.mediators.builtin.CalloutMediator;
import org.apache.synapse.mediators.builtin.ForEachMediator;
import org.apache.synapse.mediators.builtin.SendMediator;
import org.apache.synapse.mediators.eip.Target;

/**
 * The &lt;foreach&gt; element is used to split messages in Synapse to smaller
 * messages with only
 * one part of the elements described in the XPath or JsonPath expression.
 * <p/>
 * 
 * <pre>
 * &lt;foreach expression="xpath"&gt;
 *   &lt;target [to="uri"] [soapAction="qname"] [sequence="sequence_ref"]
 *          [endpoint="endpoint_ref"]&gt;
 *     &lt;sequence&gt;
 *       (mediator)+
 *     &lt;/sequence&gt;?
 *     &lt;endpoint&gt;
 *       endpoint
 *     &lt;/endpoint&gt;?
 *   &lt;/target&gt;+
 * &lt;/foreach&gt;
 * </pre>
 */
public class ForEachMediatorFactory extends AbstractMediatorFactory {

	private static final Log log =
	                               LogFactory.getLog(ForEachMediatorFactory.class);

	private static final QName FOREACH_Q =
	                                       new QName(
	                                                 SynapseConstants.SYNAPSE_NAMESPACE,
	                                                 "foreach");

	public QName getTagQName() {
		return FOREACH_Q;
	}

	@Override
	protected Mediator createSpecificMediator(OMElement elem,
	                                          Properties properties) {
		ForEachMediator mediator = new ForEachMediator();
		processAuditStatus(mediator, elem);
		OMAttribute expression = elem.getAttribute(ATT_EXPRN);

		if (expression != null) {
			try {
				mediator.setExpression(SynapseXPathFactory.getSynapseXPath(elem,
				                                                         ATT_EXPRN));
			} catch (JaxenException e) {
				handleException("Unable to build the ForEach Mediator. " +
				                        "Invalid XPath " +
				                        expression.getAttributeValue(), e);
			}
		} else {
			handleException("XPath expression is required "
			                + "for an ForEach Mediator under the \"expression\" attribute");
		}

		OMElement targetElement = elem.getFirstChildWithName(TARGET_Q);
		if (targetElement != null) {
			Target target =
			                TargetFactory.createTarget(targetElement,
			                                           properties);
			if (target != null) {
				boolean valid = validateTarget(target);
				if (!valid) {
					handleException("Target contains an endpoint OR Sequence for ForEach mediator invalid :: cannot contain Call, Send or Callout mediators");
				} else {
					// asynchronous is false since mediation happens in the same
					// original thread that invoked the mediate method.
					target.setAsynchronous(false);
					mediator.setTarget(target);
				}
			}
		} else {
			handleException("Target for ForEach mediator is required :: missing target");
		}

		return mediator;

	}

	private boolean validateTarget(Target target) {
		SequenceMediator sequence = target.getSequence();
		boolean valid = true;
		if (sequence != null) {
			List<Mediator> mediators = sequence.getList();
			for (Mediator m : mediators) {
				if (m instanceof CallMediator) {
					valid = false;
					break;
				} else if (m instanceof CalloutMediator) {
					valid = false;
					break;
				} else if (m instanceof SendMediator) {
					valid = false;
					break;
				}
			}
		}
		if ((target.getEndpoint() != null) ||
		           (target.getEndpointRef() != null)) {
			valid = false;
		}
		return valid;
	}

}
