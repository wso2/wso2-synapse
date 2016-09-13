/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.aspects.flow.statistics.structuring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StructuringArtifact {
    private int hashcode;
    private String name;
    private ArrayList<StructuringElement> list;

    public StructuringArtifact(int hashcode, String name, ArrayList<StructuringElement> list) {
        this.hashcode = hashcode;
        this.name = name;
        this.list = new ArrayList<>(list);
    }

    public ArrayList<StructuringElement> getList() {
        return list;
    }

    public int getHashcode() {
        return hashcode;
    }

    public void setList(ArrayList<StructuringElement> list) {
        this.list = list;
    }

    public void setHashcode(int hashcode) {
        this.hashcode = hashcode;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getObjectAsMap() {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("hashcode", this.hashcode);
        objectMap.put("name", this.name);
        objectMap.put("components", this.list);

        return objectMap;
    }
}
