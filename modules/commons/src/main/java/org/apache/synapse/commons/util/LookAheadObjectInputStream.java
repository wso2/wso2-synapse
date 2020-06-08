/*
Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.commons.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class LookAheadObjectInputStream extends ObjectInputStream {

    private Class<?> aClass;

    /**
     * Constructor.
     *
     * @param in     input stream to read from
     * @param aClass a <code>Class</code> object
     * @throws IOException any of the usual Input/Output exceptions
     */
    public LookAheadObjectInputStream(InputStream in, Class<?> aClass) throws IOException {
        super(in);
        this.aClass = aClass;
    }

    /**
     * @see java.io.ObjectInputStream#resolveClass(ObjectStreamClass).
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        if (!desc.getName().equals(aClass.getName())) {
            throw new InvalidClassException("Unauthorized deserialization attempt", desc.getName());
        }
        return super.resolveClass(desc);
    }
}
