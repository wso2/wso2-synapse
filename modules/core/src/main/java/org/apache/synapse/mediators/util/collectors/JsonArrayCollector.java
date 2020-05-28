/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.synapse.mediators.util.collectors;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.synapse.mediators.util.SimpleMessageContext;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.stream.Collector.Characteristics.UNORDERED;

/**
 * Collector to collect a Stream of JsonElement to a Json array and set the payload content
 */
public class JsonArrayCollector implements Collector<JsonElement, JsonArray, Boolean> {

    private final SimpleMessageContext simpleMessageContext;

    public JsonArrayCollector(SimpleMessageContext simpleMessageContext) {

        this.simpleMessageContext = simpleMessageContext;
    }

    @Override
    public Supplier<JsonArray> supplier() {

        return JsonArray::new;
    }

    @Override
    public BiConsumer<JsonArray, JsonElement> accumulator() {

        return JsonArray::add;
    }

    @Override
    public BinaryOperator<JsonArray> combiner() {

        return (array1, array2) -> {
            JsonArray newArray = new JsonArray();
            while (array1.iterator().hasNext()) {
                newArray.add(array1.iterator().next());
            }
            while (array2.iterator().hasNext()) {
                newArray.add(array2.iterator().next());
            }
            return newArray;

        };
    }

    @Override
    public Function<JsonArray, Boolean> finisher() {

        return result -> {
                simpleMessageContext.setJsonPayload(result);
            return true;
        };
    }

    @Override
    public Set<Characteristics> characteristics() {

        return Sets.immutableEnumSet(UNORDERED);
    }

}
