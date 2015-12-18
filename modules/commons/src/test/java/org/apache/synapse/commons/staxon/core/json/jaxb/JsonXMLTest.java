/*
 * Copyright 2011, 2012 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.commons.staxon.core.json.jaxb;

import junit.framework.Assert;

import org.junit.Test;

import org.apache.synapse.commons.staxon.core.json.jaxb.JsonXML;

@JsonXML
public class JsonXMLTest {
    @Test
    public void testDefaults() {
        JsonXML config = getClass().getAnnotation(JsonXML.class);
        Assert.assertFalse(config.autoArray());
        Assert.assertTrue(config.namespaceDeclarations());
        Assert.assertEquals(':', config.namespaceSeparator());
        Assert.assertEquals(0, config.multiplePaths().length);
        Assert.assertFalse(config.prettyPrint());
        Assert.assertFalse(config.virtualRoot());
    }
}
