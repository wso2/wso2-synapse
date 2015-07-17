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
package org.apache.synapse.commons.staxon.core.json;

/**
 * Constants used by reader and writer classes.
 */
public class JsonXMLStreamConstants {
    /**
     * The name of the processing instruction used to indicate collections:
     * <code>&lt;?xml-multiple bob?&gt;</code>
     * <p>When writing JSON, information about starting a "collection" as in
     * <code>&lt;alice&gt;&lt;bob&gt;charlie&lt;/bob&gt;&lt;bob&gt;david&lt;/bob&gt;&lt;/alice&gt;</code>
     * may be required by the writer. This PI may be used to pass the name
     * of the muliple element to the writer. The writer will typically close the
     * array automatically.
     * When reading JSON, a reader may report this processing instruction on array starts.
     */
    public static final String MULTIPLE_PI_TARGET = "xml-multiple";
}
