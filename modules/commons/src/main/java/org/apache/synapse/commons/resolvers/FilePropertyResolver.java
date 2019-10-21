/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.apache.synapse.commons.util.FilePropertyLoader;

/**
 *  File Property resolver can be used to resolve file property variables in the synapse config.
 */
public class FilePropertyResolver implements Resolver {

    private static final Log log = LogFactory.getLog(FilePropertyResolver.class);

    private String input;

    /**
     * set environment variable which needs to resolved
     **/
    @Override
    public void setVariable(String input) {
        this.input = input;
    }

    /**
     * file property variable is resolved in this function
     * @return resolved value for the file property variable
     */
    @Override
    public String resolve() {
        FilePropertyLoader fileLoaderObject = FilePropertyLoader.getFileLoaderInstance();
        fileLoaderObject.setFileValue(input);
        String filePropertyValue = fileLoaderObject.getFileValue();

        log.debug("resolving PropertiesFile value "+filePropertyValue);
        if (filePropertyValue == null) {
            throw new ResolverException("File Property variable could not be found");
        }
        return filePropertyValue;
    }
}
