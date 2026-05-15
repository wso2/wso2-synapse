/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
 */
package org.apache.synapse.libraries.util;

import org.apache.axis2.deployment.DeploymentException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.libraries.LibClassLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link LibDeployerUtils#addDependencyToSynapseLibrary(String, String)}.
 * <p>
 * Covers the deduplication fix for issue #4965: when multiple CARs each bundle the same
 * connector dependency JAR (e.g., {@code commons-vfs2-sandbox-2.10.0-wso2v4.jar}), the
 * second CAR's deployment must <em>not</em> add a duplicate URL to the shared
 * {@link LibClassLoader}.  Without this dedup, {@code StandardFileSystemManager.configurePlugins()}
 * encounters two copies of {@code META-INF/vfs-providers.xml} and throws
 * {@code FileSystemException: Multiple providers registered for URL scheme "smb2"},
 * which opens the circuit breaker and permanently breaks File Connector operations.
 */
public class LibDeployerUtilsTest {

    private static final String LIBRARY_NAME = "{org.wso2.carbon.connector}file";

    private Path car1Dir;
    private Path car2Dir;
    private Path car3Dir;

    @Before
    public void setUp() throws IOException {
        // Ensure the static SynapseConfiguration classloader registry is clean before each test.
        SynapseConfiguration.getLibraryClassLoaders().clear();
        car1Dir = Files.createTempDirectory("issue4965_car1_");
        car2Dir = Files.createTempDirectory("issue4965_car2_");
        car3Dir = Files.createTempDirectory("issue4965_car3_");
    }

    @After
    public void tearDown() {
        SynapseConfiguration.getLibraryClassLoaders().clear();
        deleteQuietly(car1Dir.toFile());
        deleteQuietly(car2Dir.toFile());
        deleteQuietly(car3Dir.toFile());
    }

    // -------------------------------------------------------------------------
    // Basic classloader lifecycle
    // -------------------------------------------------------------------------

    /**
     * Verifies that the first call to {@code addDependencyToSynapseLibrary} creates a
     * {@link LibClassLoader} and registers it in {@link SynapseConfiguration}.
     */
    @Test
    public void testFirstCallCreatesLibClassLoader() throws DeploymentException, IOException {
        String libName = "{org.wso2.carbon.connector}newlib";
        File jar = createTempFile(car1Dir, "newlib-1.0.0.jar");

        assertNull("No classloader should exist before the first call",
                SynapseConfiguration.getClassLoader(libName));

        LibDeployerUtils.addDependencyToSynapseLibrary(libName, jar.getAbsolutePath());

        ClassLoader loader = SynapseConfiguration.getClassLoader(libName);
        assertNotNull("ClassLoader must be created on the first call", loader);
        assertTrue("ClassLoader must be a LibClassLoader instance",
                loader instanceof LibClassLoader);
    }

    /**
     * Verifies that the first call adds the dependency JAR to the classloader URL list.
     */
    @Test
    public void testFirstCallAddsJarUrlToClassLoader() throws DeploymentException, IOException {
        File jar = createTempFile(car1Dir, "commons-vfs2-sandbox-2.10.0-wso2v4.jar");

        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, jar.getAbsolutePath());

        LibClassLoader classLoader = (LibClassLoader) SynapseConfiguration.getClassLoader(LIBRARY_NAME);
        assertEquals("First call must add exactly one URL to the classloader", 1,
                classLoader.getURLs().length);
    }

    // -------------------------------------------------------------------------
    // Deduplication — issue #4965 regression tests
    // -------------------------------------------------------------------------

    /**
     * Core regression test for issue #4965.
     * <p>
     * Simulates two CARs each bundling {@code commons-vfs2-sandbox-2.10.0-wso2v4.jar}: the JARs
     * have the same filename but live under different CAR temp-extraction paths.  After both
     * deployments the {@link LibClassLoader} must contain that filename exactly <em>once</em>.
     * <p>
     * Before the fix the second call added a duplicate URL, causing
     * {@code StandardFileSystemManager} to read {@code vfs-providers.xml} twice and throw
     * {@code "Multiple providers registered for URL scheme 'smb2'"}.
     */
    @Test
    public void testDuplicateFilenameIsNotAddedToClassLoader() throws DeploymentException, IOException {
        // Two CARs, same dependency filename, different extraction paths
        File jar1 = createTempFile(car1Dir, "commons-vfs2-sandbox-2.10.0-wso2v4.jar");
        File jar2 = createTempFile(car2Dir, "commons-vfs2-sandbox-2.10.0-wso2v4.jar");

        // CAR 1 deploys — no classloader exists yet
        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, jar1.getAbsolutePath());
        // CAR 2 deploys — classloader already exists; duplicate filename must be skipped
        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, jar2.getAbsolutePath());

        LibClassLoader classLoader = (LibClassLoader) SynapseConfiguration.getClassLoader(LIBRARY_NAME);
        assertNotNull(classLoader);

        long matchCount = countUrlsWithFilename(
                classLoader.getURLs(), "commons-vfs2-sandbox-2.10.0-wso2v4.jar");
        assertEquals(
                "Duplicate JAR filename must appear exactly once in the classloader URLs"
                        + " — regression guard for issue #4965",
                1, matchCount);
    }

    /**
     * Verifies that the deduplication holds across three CARs bundling the same dependency.
     */
    @Test
    public void testTripleDeploymentDeduplicates() throws DeploymentException, IOException {
        File jar1 = createTempFile(car1Dir, "commons-vfs2-sandbox-2.10.0-wso2v4.jar");
        File jar2 = createTempFile(car2Dir, "commons-vfs2-sandbox-2.10.0-wso2v4.jar");
        File jar3 = createTempFile(car3Dir, "commons-vfs2-sandbox-2.10.0-wso2v4.jar");

        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, jar1.getAbsolutePath());
        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, jar2.getAbsolutePath());
        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, jar3.getAbsolutePath());

        LibClassLoader classLoader = (LibClassLoader) SynapseConfiguration.getClassLoader(LIBRARY_NAME);
        long matchCount = countUrlsWithFilename(
                classLoader.getURLs(), "commons-vfs2-sandbox-2.10.0-wso2v4.jar");
        assertEquals(
                "Same JAR bundled in three CARs should appear exactly once in classloader URLs",
                1, matchCount);
    }

    // -------------------------------------------------------------------------
    // Deduplication is filename-based: distinct JARs must still be added
    // -------------------------------------------------------------------------

    /**
     * Verifies that a dependency with a <em>different</em> filename is correctly added as a
     * separate classloader entry — i.e., the dedup must not suppress genuinely distinct JARs.
     */
    @Test
    public void testDifferentFilenameIsAdded() throws DeploymentException, IOException {
        File jar1 = createTempFile(car1Dir, "commons-vfs2-2.10.0-wso2v4.jar");
        File jar2 = createTempFile(car1Dir, "commons-vfs2-sandbox-2.10.0-wso2v4.jar");

        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, jar1.getAbsolutePath());
        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, jar2.getAbsolutePath());

        LibClassLoader classLoader = (LibClassLoader) SynapseConfiguration.getClassLoader(LIBRARY_NAME);
        URL[] urls = classLoader.getURLs();
        assertEquals("Two distinct dependency JARs must each appear once in the classloader", 2,
                urls.length);
    }

    /**
     * Full two-CAR scenario with all File Connector 6 dependencies:
     * verifies that both {@code commons-vfs2} and {@code commons-vfs2-sandbox} are each
     * deduplicated independently when two CARs are deployed.
     */
    @Test
    public void testAllFileConnectorDependenciesDeduplicatedAcrossTwoCars()
            throws DeploymentException, IOException {
        // CAR 1 dependencies
        File vfsJar1     = createTempFile(car1Dir, "commons-vfs2-2.10.0-wso2v4.jar");
        File sandboxJar1 = createTempFile(car1Dir, "commons-vfs2-sandbox-2.10.0-wso2v4.jar");
        // CAR 2 — same filenames, different temp extraction path
        File vfsJar2     = createTempFile(car2Dir, "commons-vfs2-2.10.0-wso2v4.jar");
        File sandboxJar2 = createTempFile(car2Dir, "commons-vfs2-sandbox-2.10.0-wso2v4.jar");

        // CAR 1 deploys
        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, vfsJar1.getAbsolutePath());
        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, sandboxJar1.getAbsolutePath());
        // CAR 2 deploys the same deps again
        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, vfsJar2.getAbsolutePath());
        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, sandboxJar2.getAbsolutePath());

        LibClassLoader classLoader = (LibClassLoader) SynapseConfiguration.getClassLoader(LIBRARY_NAME);
        URL[] urls = classLoader.getURLs();

        assertEquals("Total unique dependency JARs should be 2 — no duplicates allowed", 2,
                urls.length);
        assertEquals("commons-vfs2 JAR must appear exactly once",
                1, countUrlsWithFilename(urls, "commons-vfs2-2.10.0-wso2v4.jar"));
        assertEquals("commons-vfs2-sandbox JAR must appear exactly once",
                1, countUrlsWithFilename(urls, "commons-vfs2-sandbox-2.10.0-wso2v4.jar"));
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    /**
     * Verifies that calling {@code addDependencyToSynapseLibrary} with the exact same path
     * twice (not just same filename) also deduplicates correctly.
     */
    @Test
    public void testExactSamePathDeduplicates() throws DeploymentException, IOException {
        File jar = createTempFile(car1Dir, "commons-vfs2-sandbox-2.10.0-wso2v4.jar");
        String path = jar.getAbsolutePath();

        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, path);
        LibDeployerUtils.addDependencyToSynapseLibrary(LIBRARY_NAME, path);

        LibClassLoader classLoader = (LibClassLoader) SynapseConfiguration.getClassLoader(LIBRARY_NAME);
        long matchCount = countUrlsWithFilename(
                classLoader.getURLs(), "commons-vfs2-sandbox-2.10.0-wso2v4.jar");
        assertEquals("Same path added twice must still produce only one URL", 1, matchCount);
    }

    /**
     * Verifies that separate library names maintain independent classloaders, so dedup for
     * one library does not interfere with another.
     */
    @Test
    public void testSeparateLibrariesHaveIndependentClassLoaders()
            throws DeploymentException, IOException {
        String libA = "{org.wso2.carbon.connector}fileA";
        String libB = "{org.wso2.carbon.connector}fileB";

        File jarA = createTempFile(car1Dir, "lib-a-1.0.0.jar");
        File jarB = createTempFile(car2Dir, "lib-b-1.0.0.jar");

        LibDeployerUtils.addDependencyToSynapseLibrary(libA, jarA.getAbsolutePath());
        LibDeployerUtils.addDependencyToSynapseLibrary(libB, jarB.getAbsolutePath());

        assertNotSame("Each library must have its own independent classloader",
                SynapseConfiguration.getClassLoader(libA),
                SynapseConfiguration.getClassLoader(libB));
        assertEquals("libA classloader must contain exactly 1 URL", 1,
                ((LibClassLoader) SynapseConfiguration.getClassLoader(libA)).getURLs().length);
        assertEquals("libB classloader must contain exactly 1 URL", 1,
                ((LibClassLoader) SynapseConfiguration.getClassLoader(libB)).getURLs().length);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static File createTempFile(Path directory, String filename) throws IOException {
        File file = directory.resolve(filename).toFile();
        file.createNewFile();
        return file;
    }

    /**
     * Counts how many URLs in the given array end with the specified filename.
     */
    private static long countUrlsWithFilename(URL[] urls, String filename) {
        long count = 0;
        for (URL url : urls) {
            String path = url.getPath();
            String urlFilename = path.substring(path.lastIndexOf('/') + 1);
            if (filename.equals(urlFilename)) {
                count++;
            }
        }
        return count;
    }

    private static void deleteQuietly(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteQuietly(child);
            }
        }
        file.delete();
    }
}
