/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.deployers;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.AbstractSynapseObserver;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.libraries.imports.SynapseImport;
import org.apache.synapse.libraries.model.Library;
import org.apache.synapse.libraries.util.LibDeployerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Two applications that carry the same library: the library stays until the last of them is undeployed. One
 * application alone deploys and undeploys its library as before.
 */
public class LibraryArtifactDeployerTest {

    private static final String PACKAGE = "org.test.library";

    private static int libraries;

    private File folder;
    private String localName;
    private String name;
    private File zip;
    private LibraryArtifactDeployer deployer;
    private SynapseConfiguration synapseConfiguration;

    @Before
    public void setUp() throws Exception {
        folder = Files.createTempDirectory("library_users_").toFile();
        // Each test has its own library name, because the count and the class loaders of the libraries are static.
        localName = "shared" + (++libraries);
        name = "{" + PACKAGE + "}" + localName;
        zip = library(new File(folder, "first"));
        synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);
        deployer = new LibraryArtifactDeployer();
        deployer.init(cfgCtx);
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(folder);
    }

    @Test
    public void theLibraryStaysWhileAnotherApplicationUsesIt() throws Exception {
        imported();
        File second = copyOf(zip, "second");
        deploy(zip);
        deploy(second);
        assertEquals("both applications are users of the library", 2, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(zip);
        assertNotNull("the library stays for the second application", library());
        assertNotNull("its template stays", template());
        assertNotNull("its local entry stays", synapseConfiguration.getLocalRegistry().get(entryKey()));
        assertNotNull("its class loader stays", SynapseConfiguration.getClassLoader(name));
        assertEquals(1, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(second);
        assertNull("the library goes with its last user", library());
        assertNull(template());
        assertNull("its class loader goes with its last user", SynapseConfiguration.getClassLoader(name));
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void theLibraryStaysWhenTheSecondApplicationGoesFirst() throws Exception {
        File second = copyOf(zip, "second");
        deploy(zip);
        deploy(second);

        undeploy(second);
        assertNotNull("the undeploy of the second application is found and the library stays", library());
        assertEquals(1, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(zip);
        assertNull(library());
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void aSecondPickOfTheSameFileIsNoNewUser() throws Exception {
        deploy(zip);
        deploy(zip);
        assertEquals("the same file picked again is no second user", 1, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(zip);
        assertNull(library());
    }

    @Test
    public void aLibraryOfOneApplicationGoesWithIt() throws Exception {
        deploy(zip);
        assertNotNull(library());

        undeploy(zip);
        assertNull(library());
        assertNull(SynapseConfiguration.getClassLoader(name));
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void aLibraryOfOneApplicationRunsEveryStepOfItsUndeploy() throws Exception {
        Calls calls = new Calls();
        synapseConfiguration.registerObserver(calls);
        imported();
        deploy(zip);
        Library deployed = library();
        assertNotNull("the template of the library is loaded", template());
        assertNotNull("the local entry of the library is deployed",
                synapseConfiguration.getLocalRegistry().get(entryKey()));
        assertTrue(store().containsFileName(zip.getAbsolutePath()));
        assertEquals(1, calls.librariesAdded);
        assertEquals(1, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(zip);
        assertNull("the library goes", library());
        assertNull("the library is unloaded", deployed.getArtifact(templateKey()));
        assertNull("the template goes", template());
        assertNull("the local entry goes", synapseConfiguration.getLocalRegistry().get(entryKey()));
        assertEquals(1, calls.entriesRemoved);
        assertNull("the import goes", synapseConfiguration.getSynapseImports().get(name));
        assertEquals("the observers hear of the removal", 1, calls.librariesRemoved);
        assertNull("the class loader goes", SynapseConfiguration.getClassLoader(name));
        assertFalse("the file leaves the deployment store", store().containsFileName(zip.getAbsolutePath()));
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void aRedeployOfOneApplicationBuildsTheLibraryAgain() throws Exception {
        deploy(zip);
        Library before = library();
        ClassLoader loaderBefore = SynapseConfiguration.getClassLoader(name);

        undeploy(zip);
        assertNull(library());
        assertNull(SynapseConfiguration.getClassLoader(name));

        File again = library(new File(folder, "again"));
        deploy(again);
        Library after = library();
        assertNotNull("the library is deployed again", after);
        assertNotSame("the redeploy builds a new library", before, after);
        assertNotSame("the redeploy builds a new class loader", loaderBefore, SynapseConfiguration.getClassLoader(name));
        assertEquals(1, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(again);
        assertNull(library());
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void fiveRedeploysOfOneApplicationKeepOneUserAndNoCopies() throws Exception {
        int copies = dependencyCopies();
        Library previous = null;
        for (int i = 1; i <= 5; i++) {
            File application = new File(folder, "redeploy" + i);
            File file = library(application);
            LibDeployerUtils.addDependencyToSynapseLibrary(name, jar(application).getAbsolutePath());
            deploy(file);
            Library current = library();
            assertNotNull(current);
            assertNotSame("each redeploy builds the library again", previous, current);
            assertEquals(1, SynapseConfiguration.getDeployedLibCount(name));
            assertEquals("one copy of the dependency while the application runs", copies + 1, dependencyCopies());

            undeploy(file);
            FileUtils.deleteQuietly(application);
            assertNull(library());
            assertNull(SynapseConfiguration.getClassLoader(name));
            assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
            assertEquals("the copy goes with the application", copies, dependencyCopies());
            previous = current;
        }
    }

    @Test
    public void anApplicationRolledBackAfterItsLibraryIsNoUser() throws Exception {
        File second = copyOf(zip, "second");
        File again = copyOf(zip, "again");
        deploy(zip);
        deploy(second);

        // A later artifact of the second application fails, so its deployed artifacts are undeployed again.
        undeploy(second);
        assertEquals(1, SynapseConfiguration.getDeployedLibCount(name));
        assertFalse(store().containsFileName(second.getAbsolutePath()));

        deploy(again);
        assertEquals(2, SynapseConfiguration.getDeployedLibCount(name));
        undeploy(zip);
        undeploy(again);
        assertNull(library());
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void anUndeployThatFailsAfterTheLibraryIsRemovedLeavesNoUser() throws Exception {
        synapseConfiguration.registerObserver(new AbstractSynapseObserver() {
            private boolean failed;

            @Override
            public void synapseLibraryRemoved(Library library) {
                if (!failed) {
                    failed = true;
                    throw new IllegalStateException("an observer fails");
                }
            }
        });
        deploy(zip);
        try {
            undeploy(zip);
            fail("the failure of the observer reaches the caller");
        } catch (IllegalStateException expected) {
            // the application undeploy logs it
        }
        assertNull(library());
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));

        File again = library(new File(folder, "again"));
        deploy(again);
        assertNotNull("the application deployed again from a new folder builds the library", library());
        assertEquals(1, SynapseConfiguration.getDeployedLibCount(name));
        undeploy(again);
        assertNull(library());
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void aLibraryLeftByAnUndeployThatFailsEarlyStaysUntilARestart() throws Exception {
        synapseConfiguration.registerObserver(new AbstractSynapseObserver() {
            private boolean failed;

            @Override
            public void entryRemoved(Entry entry) {
                if (!failed) {
                    failed = true;
                    throw new IllegalStateException("an observer fails");
                }
            }
        });
        imported();
        deploy(zip);
        Library left = library();
        try {
            undeploy(zip);
            fail("the failure of the observer reaches the caller");
        } catch (IllegalStateException expected) {
            // the application undeploy logs it
        }
        assertSame("the library is left in place, as without the count", left, library());

        File again = library(new File(folder, "again"));
        deploy(again);
        assertSame("the application deployed again runs on the library left in place", left, library());
        undeploy(again);
        assertSame("the library left in place stays until a restart, as without the count", left, library());
    }

    @Test
    public void aUserReplacedInPlaceAndThenRemovedGivesItsUserBack() throws Exception {
        File libs = copyOf(zip, "synapse-libs");
        deploy(zip);
        deploy(libs);
        assertEquals(2, SynapseConfiguration.getDeployedLibCount(name));

        // The file in the folder of the libraries is replaced in place: the hot deployer undeploys it while the file
        // is there, then deploys it again.
        deployer.undeploy(libs.getAbsolutePath());
        deploy(libs);
        assertEquals(2, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(libs);
        assertEquals("the removed file is no user any more", 1, SynapseConfiguration.getDeployedLibCount(name));
        assertNotNull(library());

        undeploy(zip);
        assertNull("the library goes with its last user", library());
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void aLoneLibraryReplacedInPlaceAndThenRemovedIsKeptAsBefore() throws Exception {
        deploy(zip);
        deployer.undeploy(zip.getAbsolutePath());
        deploy(zip);
        try {
            undeploy(zip);
            fail("the removed file is not found, as before");
        } catch (DeploymentException expected) {
            // the library stays until a restart, as before
        }
        assertNotNull(library());
        assertEquals(1, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void anotherVersionRunsOnTheLibraryInUse() throws Exception {
        File other = new File(new File(folder, "second"), zip.getName().replace("1.0.0", "2.0.0"));
        FileUtils.copyFile(zip, other);
        deploy(zip);
        Library inUse = library();

        deploy(other);
        assertSame("the library deployed first stays in use", inUse, library());
        assertEquals(2, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(other);
        assertSame(inUse, library());
        undeploy(zip);
        assertNull(library());
    }

    private void deploy(File file) throws DeploymentException {
        deployer.deploy(new DeploymentFileData(file, deployer));
    }

    // An application undeploy removes the file first, then the deployer undeploys it.
    private void undeploy(File file) throws DeploymentException {
        FileUtils.deleteQuietly(file);
        deployer.undeploy(file.getAbsolutePath());
    }

    private Library library() {
        return synapseConfiguration.getSynapseLibraries().get(name);
    }

    private SynapseArtifactDeploymentStore store() {
        return synapseConfiguration.getArtifactDeploymentStore();
    }

    private String templateKey() {
        return PACKAGE + "." + localName + ".hello";
    }

    private String entryKey() {
        return localName + ".greeting";
    }

    private Object template() {
        return LibDeployerUtils.getLibArtifact(synapseConfiguration.getSynapseLibraries(), templateKey(), Object.class);
    }

    // The import that makes the library load its templates and local entries when it is deployed.
    private void imported() {
        SynapseImport synapseImport = new SynapseImport();
        synapseImport.setLibName(localName);
        synapseImport.setLibPackage(PACKAGE);
        synapseImport.setStatus(true);
        synapseConfiguration.addSynapseImport(name, synapseImport);
    }

    private static int dependencyCopies() {
        File[] copies = new File(LibDeployerUtils.APP_UNZIP_DIR).listFiles(
                file -> file.isDirectory() && file.getName().endsWith("-dependency"));
        return copies == null ? 0 : copies.length;
    }

    private static File copyOf(File source, String application) throws IOException {
        File copy = new File(new File(source.getParentFile().getParentFile(), application), source.getName());
        FileUtils.copyFile(source, copy);
        return copy;
    }

    private static File jar(File application) throws IOException {
        File file = new File(application, "provider-1.0.0.jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(file))) {
            out.putNextEntry(new ZipEntry("META-INF/test-providers.xml"));
            out.write("<providers/>".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return file;
    }

    // A library with one template and one local entry, laid out as the library deployer reads it.
    private File library(File application) throws IOException {
        File file = new File(application, localName + "-connector-1.0.0.zip");
        application.mkdirs();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
            add(out, "connector.xml", "<connector><component name=\"" + localName + "\" package=\"" + PACKAGE + "\">"
                    + "<dependency component=\"greet\"/><description>a test library</description>"
                    + "</component></connector>");
            out.putNextEntry(new ZipEntry("greet/"));
            out.closeEntry();
            add(out, "greet/component.xml", "<component name=\"greet\" type=\"synapse/template\"><subComponents>"
                    + "<component name=\"hello\"><file>hello.xml</file><description>a template</description>"
                    + "</component></subComponents><description>templates</description></component>");
            add(out, "greet/hello.xml", "<template xmlns=\"http://ws.apache.org/ns/synapse\" name=\"hello\">"
                    + "<sequence><log/></sequence></template>");
            out.putNextEntry(new ZipEntry("local-entries/"));
            out.closeEntry();
            add(out, "local-entries/greeting.xml", "<localEntry xmlns=\"http://ws.apache.org/ns/synapse\""
                    + " key=\"greeting\">hello</localEntry>");
        }
        return file;
    }

    private static void add(ZipOutputStream out, String entry, String text) throws IOException {
        out.putNextEntry(new ZipEntry(entry));
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }

    private static class Calls extends AbstractSynapseObserver {

        private int librariesAdded;
        private int librariesRemoved;
        private int entriesRemoved;

        @Override
        public void synapseLibraryAdded(Library library) {
            librariesAdded++;
        }

        @Override
        public void synapseLibraryRemoved(Library library) {
            librariesRemoved++;
        }

        @Override
        public void entryRemoved(Entry entry) {
            entriesRemoved++;
        }
    }
}
