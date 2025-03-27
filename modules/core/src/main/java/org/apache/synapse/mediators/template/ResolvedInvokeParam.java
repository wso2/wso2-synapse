/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.template;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class represents the resolved value from the {@link InvokeParam} of the sequence template.
 */
public class ResolvedInvokeParam {

    private String name;
    private Object inlineValue;
    private Map<String, Object> attributes;
    List<ResolvedInvokeParam> children;

    public ResolvedInvokeParam() {

        attributes = new LinkedHashMap<>();
        children = new LinkedList<>();
    }

    public String getParamName() {

        return name;
    }

    public void setParamName(String paramName) {

        this.name = paramName;
    }

    public Object getInlineValue() {

        return inlineValue;
    }

    public void setInlineValue(Object inlineValue) {

        this.inlineValue = inlineValue;
    }

    public Map<String, Object> getAttributes() {

        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {

        this.attributes = attributes;
    }

    public void addAttribute(String key, Object value) {

        attributes.put(key, value);
    }

    public List<ResolvedInvokeParam> getChildren() {

        return children;
    }

    public void setChildren(List<ResolvedInvokeParam> children) {

        this.children = children;
    }

    public void addChildParam(ResolvedInvokeParam childParam) {

        children.add(childParam);
    }
}
