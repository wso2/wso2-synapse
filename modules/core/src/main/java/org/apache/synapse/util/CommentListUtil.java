/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.util;

import org.apache.axiom.om.OMComment;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMAbstractFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Contains the utils that are used to handle comments in synapse
 *
 */
public class CommentListUtil {

    /**
     * Adds comments in Synapse to a list of comments
     *
     * @param el    OMElement containing the comments
     * @param commentList List to be which the comments should be added to
     */
    public static void populateComments(OMElement el, List<String> commentList) {
        Iterator it = el.getChildren();

        while (it.hasNext()) {
            OMNode child = (OMNode) it.next();
            if (child instanceof OMComment && ((OMComment) child).getValue() != null) {
                commentList.add(((OMComment) child).getValue());
            }
        }
    }

    /**
     * Serialize string comment entries from a List
     *
     * @param parent      OMElement to be updated
     * @param commentList List of comment entries to be serialized
     */
    public static void serializeComments(OMElement parent, List<String> commentList) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        for (String comment : commentList) {
            OMComment commendNode = fac.createOMComment(parent, "comment");
            commendNode.setValue(comment);
            parent.addChild(commendNode);
        }
    }
}
