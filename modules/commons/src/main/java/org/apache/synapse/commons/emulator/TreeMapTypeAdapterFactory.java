/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.commons.emulator;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class responsible for holding the JSON data as a tree-map.
 */
public class TreeMapTypeAdapterFactory implements TypeAdapterFactory {

    public final <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (Map.class.isAssignableFrom(type.getRawType())) {
            final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            return createCustomTypeAdapter(delegate);
        }
        return null;
    }

    private <T> TypeAdapter<T> createCustomTypeAdapter(final TypeAdapter<T> delegate) {
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                Map map = (Map) value;
                delegate.write(out, (T) new TreeMap(map));
            }

            @Override
            public T read(JsonReader in) throws IOException {
                return delegate.read(in);
            }
        };
    }
}
