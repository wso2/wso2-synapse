/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.config.xml.endpoints.resolvers;

import org.apache.synapse.ServerManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  System resolver can be used to resolve environment variables in the synapse config.
 */
public class SystemResolver extends Resolver {

    private static final int ENV_VARIABLE_INDEX = 3;

    public SystemResolver(Pattern pattern) {

        super(pattern);
    }

    private boolean isEnvironmentVariable(String variable) {
        Matcher matcher = getPattern().matcher(variable);
        if (matcher.find()) {
            if (System.getenv(matcher.group(ENV_VARIABLE_INDEX)) == null) {
                throw new ResolverException("Environment variable could not be found");
            }
            return true;
        }
        return false;
    }

    private String getEnvironmentVariableValue(String variable) {
        Matcher matcher = getPattern().matcher(variable);
        if (matcher.find()) {
            if (System.getenv(matcher.group(ENV_VARIABLE_INDEX)) == null) {
                throw new ResolverException("Environment variable could not be found");
            }
            return System.getenv(matcher.group(ENV_VARIABLE_INDEX));
        }
        return "";
    }

    /**
     * environment variable is resolved in this function
     * @param input environment variable
     * @return resolved value for the environment variable
     */
    @Override
    public String resolve(String input) {

        String envValue = input;
        if (isEnvironmentVariable(input)) {
            envValue = getEnvironmentVariableValue(input);
        }
        return envValue;
    }
}
