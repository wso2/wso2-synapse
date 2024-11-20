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
package org.apache.synapse.commons.resolvers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.ServiceLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Resolver Factory which can be used to register resolvers and retrieve a resolver for a given type.
 */
public class ResolverFactory {

    private static final Log LOG = LogFactory.getLog(ResolverFactory.class);
    private static final int RESOLVER_INDEX = 2;
    private static final ResolverFactory resolverFactory = new ResolverFactory();
    private final Pattern rePattern = Pattern.compile("(\\$)([_a-zA-Z0-9]+):([_a-zA-Z0-9]+)");
    private static final String SYSTEM_VARIABLE_PREFIX = "$SYSTEM";
    private static final String FILE_PROPERTY_VARIABLE_PREFIX = "$FILE";
    private static final String CUSTOM_PROPERTY_VARIABLE_PREFIX = "$CUSTOM_";
    private static final String CONFIGURABLE_VARIABLE_PREFIX = "$config:";

    private final Map<String, Class<? extends Resolver>> resolverMap = new HashMap<>();

    /**
     * This function return an object of Resolver factory
     * @return resolverFactory
     */
    public static ResolverFactory getInstance() {
        return resolverFactory;
    }

    private ResolverFactory() {
        registerResolvers();
        registerExterns();
    }

    /**
     * This function returns a resolver based on the variable passed
     * @param input variable which is used decode and retrieve the resolver
     * @return resolver object
     */
    public Resolver getResolver(String input) {

        if (input == null) {
            return null;
        }
        Matcher matcher = rePattern.matcher(input);
        if (input.startsWith(CONFIGURABLE_VARIABLE_PREFIX)) {
            if (matcher.find()) {
                return getResolver(matcher.group(RESOLVER_INDEX).toLowerCase(), matcher);
            }
        } else if (input.startsWith(SYSTEM_VARIABLE_PREFIX)) {
            if (matcher.find()) {
                return getResolver(matcher.group(RESOLVER_INDEX).toLowerCase(), matcher);
            }
        } else if(input.startsWith(FILE_PROPERTY_VARIABLE_PREFIX)) {
            if (matcher.find()) {
                return getResolver(matcher.group(RESOLVER_INDEX).toLowerCase(), matcher);
            }
        } else if(input.startsWith(CUSTOM_PROPERTY_VARIABLE_PREFIX)) {
            if (matcher.find()){
                String nameWithPlaceholder = matcher.group(RESOLVER_INDEX).toLowerCase();
                String className = nameWithPlaceholder.substring(CUSTOM_PROPERTY_VARIABLE_PREFIX.length() - 1);
                if (matcher.find()) {
                    return getResolver(className, matcher);
                }
            }
        }

        Resolver resolver = new DefaultResolver();
        resolver.setVariable(input);
        return resolver;

    }

    private void registerResolvers() {
        resolverMap.put("system", SystemResolver.class);
        resolverMap.put("file", FilePropertyResolver.class);
        resolverMap.put("config", ConfigResolver.class);
    }

    private void registerExterns() {
        ServiceLoader<Resolver> loaders = ServiceLoader.load(Resolver.class);
        for (Resolver resolver : loaders) {
            String className = resolver.getClass().getName();
            String[] packageList = className.split("\\.");
            className = packageList[packageList.length - 1];
            if (resolverMap.get(className.toLowerCase()) == null) {
                resolverMap.put(className.toLowerCase(), resolver.getClass());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Added Resolver " + className + " to resolver factory ");
                }
            }
            else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed to Resolver " + className + " to resolver factory. Already exist");
                }
            }
        }
    }

    private Resolver getResolver(String className, Matcher matcher) {
        Class<? extends Resolver> resolverClass = resolverMap.get(className);
        if (resolverClass != null) {
            try {
                Resolver resolverObject = resolverClass.newInstance();
                resolverObject.setVariable(matcher.group(3));
                return resolverObject;
            } catch (IllegalAccessException | InstantiationException e) {
                throw new ResolverException("Resolver could not be initialized", e);
            }
        } else {
            throw new ResolverException("Resolver could not be found");
        }
    }
}
