/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.apache.synapse.libraries;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.ArrayList;

public class LibClassLoader extends URLClassLoader {

    public LibClassLoader(URL[] urls, ClassLoader parent) {

        super(urls, parent);
    }

    public void addURL(URL url) {

        super.addURL(url);
    }

    /**
     * If a path of a jar is given, this method will add the jar to the classpath
     * If the path is a directory, it will add all the jars in the directory to the classpath
     *
     * @param path directory to be added
     * @throws MalformedURLException
     */
    public void addToClassPath(String path) throws MalformedURLException {

        File file = new File(path);
        ArrayList urls = new ArrayList();
        urls.add(file.toURL());
        File libfiles = new File(file, "lib");
        if (!addFiles(urls, libfiles)) {
            libfiles = new File(file, "Lib");
            addFiles(urls, libfiles);
        }
        for (int i = 0; i < urls.size(); ++i) {
            super.addURL((URL) urls.get(i));
        }
    }

    private static boolean addFiles(ArrayList urls, final File libFiles) throws MalformedURLException {

        if (libFiles.exists() && libFiles.isDirectory()) {
            File[] files = libFiles.listFiles();
            for (int i = 0; i < files.length; ++i) {
                if (files[i].getName().endsWith(".jar")) {
                    urls.add(files[i].toURL());
                }
            }
            return true;
        } else {
            return false;
        }
    }

}
