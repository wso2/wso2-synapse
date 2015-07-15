/*
 * Copyright 2011, 2012 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.commons.staxon.core.json.stream;

import org.apache.synapse.commons.staxon.core.json.stream.impl.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import javax.xml.stream.FactoryConfigurationError;

/**
 * Abstract JSON stream ({@link JsonStreamSource} and {@link JsonStreamTarget}) factory.
 * <p/>
 * <p>This class provides the static {@link #newFactory()} method to lookup and instantiate a default
 * implementation using the Services API (as detailed in the JAR specification).</p>
 */
public abstract class JsonStreamFactory {
    private static String getMetaInfServicesClassName(Class<?> serviceInterface, ClassLoader classLoader) {
        String serviceId = "META-INF/services/" + serviceInterface.getName();
        InputStream input = classLoader.getResourceAsStream(serviceId);
        if (input != null) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
                try {
                    return reader.readLine();
                } catch (IOException e) {
                    // do nothing
                } finally {
                    try {
                        reader.close();
                    } catch (Exception io) {
                        // do nothing
                    }
                }
            } catch (IOException e) {
                try {
                    input.close();
                } catch (Exception io) {
                    // do nothing
                }
            }
        }
        return null;
    }

    private static String getJavaHomeLibClassName(Class<?> serviceInterface, String bundleName) {
        String home = System.getProperty("java.home");
        if (home != null) {
            InputStream input = null;
            String path = home + File.separator + "lib" + File.separator + bundleName + ".properties";
            File file = new File(path);
            try {
                if (file.exists()) {
                    input = new FileInputStream(file);
                    Properties props = new Properties();
                    props.load(input);
                    return props.getProperty(serviceInterface.getName());
                }
            } catch (IOException e) {
                // do nothing
            } catch (SecurityException e) {
                // do nothing
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException io) {
                        // do nothing
                    }
                }
            }
        }
        return null;
    }

    /**
     * <p>Create a new instance of a JsonStreamFactory.</p>
     * <p>Determines the class to instantiate as follows:
     * <ol>
     * <li>Use the Services API (as detailed in the JAR specification). If a resource with the name
     * of META-INF/services/de.odysseus.staxon.json.stream.JsonStreamFactory exists, then its first line,
     * if present, is used as the UTF-8 encoded name of the implementation class.</li>
     * <li>Use the properties file "lib/staxon.properties" in the JRE directory. If this file exists
     * and  is readable by the java.util.Properties.load(InputStream) method, and it contains an entry
     * whose key is "de.odysseus.staxon.json.stream.JsonStreamFactory", then the value of that entry is
     * used as the name of the implementation class.</li>
     * <li>Use the de.odysseus.staxon.json.stream.JsonStreamFactory system property. If a system property
     * with this name is defined, then its value is used as the name of the implementation class.</li>
     * <li>Use platform default: "de.odysseus.staxon.json.stream.impl.JsonStreamFactoryImpl".</li>
     * </ol>
     * </p>
     *
     * @return An instance of JsonStreamFactory.
     * @throws FactoryConfigurationError if a factory class cannot be found or instantiation fails.
     */
    public static JsonStreamFactory newFactory() throws FactoryConfigurationError {
        ClassLoader classLoader;
        try {
            classLoader = Thread.currentThread().getContextClassLoader();
        } catch (SecurityException e) {
            classLoader = JsonStreamFactory.class.getClassLoader();
        }

        String className = getMetaInfServicesClassName(JsonStreamFactory.class, classLoader);

        if (className == null || className.trim().length() == 0) {
            className = getJavaHomeLibClassName(JsonStreamFactory.class, "staxon");
        }

        if (className == null || className.trim().length() == 0) {
            try {
                className = System.getProperty(JsonStreamFactory.class.getName());
            } catch (Exception se) {
                // do nothing
            }
        }

        if (className == null || className.trim().length() == 0) {
            className = "org.apache.synapse.commons.staxon.core.json.stream.impl.JsonStreamFactoryImpl";
        }

        try {
            return (JsonStreamFactory) classLoader.loadClass(className).newInstance();
        } catch (Throwable e) {
            throw new FactoryConfigurationError("Error creating stream factory: " + e);
        }
    }

    /**
     * Create stream source.
     *
     * @param input
     * @return stream source
     * @throws IOException
     */
    public abstract JsonStreamSource createJsonStreamSource(InputStream input) throws IOException;

    /**
     * Create stream source.
     *
     * @param input
     * @param scanner JSON Scanner to use
     * @return stream source
     * @throws IOException
     */
    public abstract JsonStreamSource createJsonStreamSource(InputStream input, Constants.SCANNER scanner) throws IOException;

    /**
     * Create stream source.
     *
     * @param reader
     * @return stream source
     * @throws IOException
     */
    public abstract JsonStreamSource createJsonStreamSource(Reader reader) throws IOException;

    /**
     * Create stream source.
     *
     * @param reader
     * @param scanner JSON Scanner to use
     * @return stream source
     * @throws IOException
     */
    public abstract JsonStreamSource createJsonStreamSource(Reader reader, Constants.SCANNER scanner) throws IOException;

    /**
     * Create stream target.
     *
     * @param output
     * @param pretty
     * @return stream target
     * @throws IOException
     */
    public abstract JsonStreamTarget createJsonStreamTarget(OutputStream output, boolean pretty) throws IOException;

    /**
     * Create stream target.
     *
     * @param writer
     * @param pretty
     * @return stream target
     * @throws IOException
     */
    public abstract JsonStreamTarget createJsonStreamTarget(Writer writer, boolean pretty) throws IOException;
}
