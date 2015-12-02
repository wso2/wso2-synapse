/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.newstatistics.util;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * This class is used to obtain clustering information when collecting mediation statistics in cluster.
 */
public class ClusterInformationProvider {
	private static final Log log = LogFactory.getLog(ClusterInformationProvider.class);

	/**
	 * Check whether clustering is enabled in axis2.xml.
	 *
	 * @return true if clustering is enabled
	 */
	public boolean isClusteringEnabled() {
		try {
			InputStream in = new FileInputStream("./repository/conf/axis2/axis2.xml");
			OMElement results = OMXMLBuilderFactory.createOMBuilder(in).getDocumentElement();
			AXIOMXPath xpathExpression = new AXIOMXPath("/axisconfig/clustering/@enable");
			List nodeList = xpathExpression.selectNodes(results);
			if (!nodeList.isEmpty()) {
				OMAttribute attribute = (OMAttribute) nodeList.get(0);
				if (attribute.getAttributeValue().equals("true")) {
					return true;
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Clustering is disabled in axis2.xml. Statistics will be collected without hostname" +
						          " and port.");
					}
				}
			}
		} catch (FileNotFoundException | JaxenException ignored) {
			log.error("Error occurred while reading clustering information from axis2.xml.");
		}
		return false;
	}

	/**
	 * Provide localMemberHost of the node as specified in axis2.xml.
	 *
	 * @return localMemberHost in the cluster
	 */
	public String getLocalMemberHostName() {
		try {
			InputStream in = new FileInputStream("./repository/conf/axis2/axis2.xml");
			OMElement results = OMXMLBuilderFactory.createOMBuilder(in).getDocumentElement();
			AXIOMXPath xpathExpression = new AXIOMXPath("/axisconfig/clustering/parameter[@name='localMemberHost']");
			List nodeList = xpathExpression.selectNodes(results);
			if (!nodeList.isEmpty()) {
				OMElement localMemberHost = (OMElement) nodeList.get(0);
				return localMemberHost.getText();
			}
		} catch (FileNotFoundException | JaxenException ignored) {
			log.error("Error occurred while reading localMemberHost information from axis2.xml.");
		}
		return null;
	}

	/**
	 * Provide localMemberPort of the node as specified in axis2.xml.
	 *
	 * @return localMemberPort in the cluster
	 */
	public String getLocalMemberPort() {
		try {
			InputStream in = new FileInputStream("./repository/conf/axis2/axis2.xml");
			OMElement results = OMXMLBuilderFactory.createOMBuilder(in).getDocumentElement();
			AXIOMXPath xpathExpression = new AXIOMXPath("/axisconfig/clustering/parameter[@name='localMemberPort']");
			List nodeList = xpathExpression.selectNodes(results);
			if (!nodeList.isEmpty()) {
				OMElement localMemberPort = (OMElement) nodeList.get(0);
				return localMemberPort.getText();
			}
		} catch (FileNotFoundException | JaxenException ignored) {
			log.error("Error occurred while reading localMemberPort information from axis2.xml.");
		}
		return null;
	}
}
