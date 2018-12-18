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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.xml.MediatorFactoryFinder;
import java.util.ServiceLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Resolver Factory which can be used to register resolvers and retrieve a resolver for a given type.
 */
public class ResolverFactory {

    private final int RESOLVER_INDEX = 2;
    private static ResolverFactory resolverFactory = new ResolverFactory();
    private final Pattern startPattern = Pattern.compile("(^[a-zA-Z0-9])|(^$)");
    private final Pattern rePattern = Pattern.compile("(\\$)([a-zA-Z0-9]+):([_a-zA-Z0-9]+)");
    private static final Log log = LogFactory.getLog(MediatorFactoryFinder.class);

    private Map<String, Class<? extends Resolver>> resolverMap = new HashMap<>();

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
        Matcher matcher = startPattern.matcher(input);
        if (matcher.find()) {
            Resolver resolver = new DefaultResolver();
            resolver.setVariable(input);
            return resolver;
        }
        matcher = rePattern.matcher(input);
        Resolver resolverObject = null;
        if (matcher.find()) {
            Class<? extends Resolver> resolverClass = resolverMap.get(matcher.group(RESOLVER_INDEX).toLowerCase());
            if (resolverClass != null) {
                try {
                    resolverObject = resolverClass.newInstance();
                    resolverObject.setVariable(matcher.group(3));
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new ResolverException("Resolver could not be found");
                }
            }
        }
        if (resolverObject != null) {
            return resolverObject;
        }
        else {
            throw new ResolverException("Resolver could not be found");
        }
    }

    private void registerResolvers() {
        resolverMap.put("system", SystemResolver.class);
    }

    private void registerExterns() {
        ServiceLoader<Resolver> loaders = ServiceLoader.load(Resolver.class);
        for (Resolver resolver : loaders) {
            String className = resolver.getClass().getName();
            String[] packageList = className.split(".");
            className = packageList[packageList.length - 1];
            if (resolverMap.get(className.toLowerCase()) == null) {
                resolverMap.put(className.toLowerCase(), resolver.getClass());
                if (log.isDebugEnabled()) {
                    log.debug("Added Resolver " + className + " to resolver factory ");
                }
            }
            else {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to Resolver " + className + " to resolver factory. Already exist");
                }
            }
        }
    }
}