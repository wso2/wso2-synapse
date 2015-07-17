/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.axiom.om.OMElement;

/**
 * Test case for Comment Serializer. It adds comment String to the given OMElement as a OMComment
 * node
 */
public class CommentSerializerTest extends AbstractTestCase {

    public void testProxyServiceSerializationSenarioOne() throws Exception {
        String inputXml = "<definitions></definitions>";
        String outputXml = "<definitions><!--TestComment--></definitions>";

        OMElement inputOM = createOMElement(inputXml);
        String testComment = "TestComment";
        CommentSerializer.serializeComment(inputOM, testComment);
        OMElement outputOM = createOMElement(outputXml);

        assertTrue(compare(inputOM, outputOM));
    }
}
