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
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.libraries.model.Library;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Two applications that carry the same library: the library stays until the last of them is undeployed.
 */
public class LibraryArtifactDeployerTest {

    private static int libraries;

    private File folder;
    private String name;
    private File zip;
    private LibraryArtifactDeployer deployer;
    private SynapseConfiguration synapseConfiguration;

    @Before
    public void setUp() throws Exception {
        folder = Files.createTempDirectory("library_users_").toFile();
        // Each test has its own library name, because the count and the class loaders of the libraries are static.
        String localName = "shared" + (++libraries);
        name = "{org.test.library}" + localName;
        zip = library(new File(folder, "first"), localName);
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
        File second = copyOf(zip, "second");
        deploy(zip);
        deploy(second);
        assertEquals("both applications are users of the library", 2, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(zip);
        assertNotNull("the library stays for the second application", synapseConfiguration.getSynapseLibraries().get(name));
        assertNotNull("its class loader stays", SynapseConfiguration.getClassLoader(name));
        assertEquals(1, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(second);
        assertNull("the library goes with its last user", synapseConfiguration.getSynapseLibraries().get(name));
        assertNull("its class loader goes with its last user", SynapseConfiguration.getClassLoader(name));
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void theLibraryStaysWhenTheSecondApplicationGoesFirst() throws Exception {
        File second = copyOf(zip, "second");
        deploy(zip);
        deploy(second);

        undeploy(second);
        assertNotNull("the undeploy of the second application is found and the library stays",
                synapseConfiguration.getSynapseLibraries().get(name));
        assertEquals(1, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(zip);
        assertNull(synapseConfiguration.getSynapseLibraries().get(name));
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void aSecondPickOfTheSameFileIsNoNewUser() throws Exception {
        deploy(zip);
        deploy(zip);
        assertEquals("the same file picked again is no second user", 1, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(zip);
        assertNull(synapseConfiguration.getSynapseLibraries().get(name));
    }

    @Test
    public void aLibraryOfOneApplicationGoesWithIt() throws Exception {
        deploy(zip);
        assertNotNull(synapseConfiguration.getSynapseLibraries().get(name));

        undeploy(zip);
        assertNull(synapseConfiguration.getSynapseLibraries().get(name));
        assertNull(SynapseConfiguration.getClassLoader(name));
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void aRedeployOfOneApplicationBuildsTheLibraryAgain() throws Exception {
        deploy(zip);
        Library before = synapseConfiguration.getSynapseLibraries().get(name);
        ClassLoader loaderBefore = SynapseConfiguration.getClassLoader(name);

        undeploy(zip);
        assertNull(synapseConfiguration.getSynapseLibraries().get(name));
        assertNull(SynapseConfiguration.getClassLoader(name));

        File again = library(new File(folder, "again"), zip.getName().substring(0, zip.getName().indexOf('-')));
        deploy(again);
        Library after = synapseConfiguration.getSynapseLibraries().get(name);
        assertNotNull("the library is deployed again", after);
        assertNotSame("the redeploy builds a new library", before, after);
        assertNotSame("the redeploy builds a new class loader", loaderBefore, SynapseConfiguration.getClassLoader(name));
        assertEquals(1, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(again);
        assertNull(synapseConfiguration.getSynapseLibraries().get(name));
        assertEquals(0, SynapseConfiguration.getDeployedLibCount(name));
    }

    @Test
    public void anotherVersionRunsOnTheLibraryInUse() throws Exception {
        File other = new File(new File(folder, "second"), zip.getName().replace("1.0.0", "2.0.0"));
        FileUtils.copyFile(zip, other);
        deploy(zip);
        Library inUse = synapseConfiguration.getSynapseLibraries().get(name);

        deploy(other);
        assertSame("the library deployed first stays in use", inUse, synapseConfiguration.getSynapseLibraries().get(name));
        assertEquals(2, SynapseConfiguration.getDeployedLibCount(name));

        undeploy(other);
        assertSame(inUse, synapseConfiguration.getSynapseLibraries().get(name));
        undeploy(zip);
        assertNull(synapseConfiguration.getSynapseLibraries().get(name));
    }

    private void deploy(File file) throws DeploymentException {
        deployer.deploy(new DeploymentFileData(file, deployer));
    }

    // An application undeploy removes the file first, then the deployer undeploys it.
    private void undeploy(File file) throws DeploymentException {
        FileUtils.deleteQuietly(file);
        deployer.undeploy(file.getAbsolutePath());
    }

    private static File copyOf(File source, String application) throws IOException {
        File copy = new File(new File(source.getParentFile().getParentFile(), application), source.getName());
        FileUtils.copyFile(source, copy);
        return copy;
    }

    // A library with its descriptor only, as the library deployer reads it.
    private static File library(File application, String localName) throws IOException {
        File file = new File(application, localName + "-connector-1.0.0.zip");
        application.mkdirs();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
            out.putNextEntry(new ZipEntry("connector.xml"));
            out.write(("<connector><component name=\"" + localName + "\" package=\"org.test.library\">"
                    + "<description>a test library</description></component></connector>")
                    .getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return file;
    }
}
