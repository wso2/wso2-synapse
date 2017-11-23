/*
 *     Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *     WSO2 Inc. licenses this file to you under the Apache License,
 *     Version 2.0 (the "License"); you may not use this file except
 *     in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.synapse.transport.vfs;

import junit.framework.TestCase;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.base.ProtocolEndpoint;
import org.apache.axis2.transport.testkit.axis2.TransportDescriptionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileType;
import org.apache.synapse.commons.vfs.VFSConstants;
import org.junit.Assert;
import org.wso2.carbon.inbound.endpoint.protocol.file.MockFile;
import org.wso2.carbon.inbound.endpoint.protocol.file.MockFileHolder;

import java.lang.reflect.Field;

/**
 * Unit testcase to test functionality {@link VFSTransportListener} and {@link PollTableEntry}
 */
public class VFSTransportListenerTest extends TestCase {

    private static Log log = LogFactory.getLog(VFSTransportListenerTest.class);

    /**
     * Testcase to test basic functionality of {@link VFSTransportListener}
     * @throws Exception
     */
    public void testVFSTransportListenerBasics() throws Exception {

        MockFileHolder.getInstance().clear();

        String fileUri = "test1:///foo/bar/test-" + System.currentTimeMillis() + "/DIR/IN/";
        String moveAfterFailure = "test1:///foo/bar/test-" + System.currentTimeMillis() + "/DIR/FAIL/";

        AxisService axisService = new AxisService("testVFSService");
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_FILE_FILE_URI, fileUri));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_FILE_CONTENT_TYPE, "text/xml"));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_FILE_FILE_NAME_PATTERN, ".*\\.txt"));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_FILE_ACTION_AFTER_PROCESS, VFSTransportListener.MOVE));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_FILE_ACTION_AFTER_FAILURE, VFSTransportListener.MOVE));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_FILE_MOVE_AFTER_FAILURE, moveAfterFailure));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_FILE_MOVE_AFTER_FAILED_MOVE, moveAfterFailure));
        axisService.addParameter(new Parameter(VFSConstants.STREAMING, "false"));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_FILE_LOCKING, VFSConstants.TRANSPORT_FILE_LOCKING_ENABLED));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_FILE_INTERVAL, "1000"));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_FILE_COUNT, "1"));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_AUTO_LOCK_RELEASE, "true"));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_AUTO_LOCK_RELEASE_INTERVAL, "20000"));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_AUTO_LOCK_RELEASE_SAME_NODE, "true"));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_DISTRIBUTED_LOCK, "true"));
        axisService.addParameter(new Parameter(VFSConstants.TRANSPORT_DISTRIBUTED_LOCK_TIMEOUT, "20000"));
        axisService.addParameter(new Parameter(VFSConstants.FILE_SORT_PARAM, VFSConstants.FILE_SORT_VALUE_NAME));
        axisService.addParameter(new Parameter(VFSConstants.FILE_SORT_ORDER, "true"));

        TransportDescriptionFactory transportDescriptionFactory = new VFSTransportDescriptionFactory();
        TransportInDescription transportInDescription = null;
        try {
            transportInDescription = transportDescriptionFactory.createTransportInDescription();
        } catch (Exception e) {
            Assert.fail("Error occurred while creating transport in description");
        }

        VFSTransportListener vfsTransportListener = getListener(transportInDescription);
        //initialize listener
        vfsTransportListener.init(new ConfigurationContext(new AxisConfiguration()), transportInDescription);
        //Initialize VFSTransportListener
        vfsTransportListener.doInit();
        //Start listener
        vfsTransportListener.start();

        //Create poll entry
        PollTableEntry pollTableEntry = vfsTransportListener.createEndpoint();

        Assert.assertTrue("Global file locking not applied to created poll entry", pollTableEntry.isFileLockingEnabled());

        //Load configuration of poll entry
        pollTableEntry.loadConfiguration(axisService);

        populatePollTableEntry(pollTableEntry, axisService, vfsTransportListener);

        vfsTransportListener.poll(pollTableEntry);

        MockFile targetDir = MockFileHolder.getInstance().getFile(fileUri);
        Assert.assertNotNull("Failed target directory creation", targetDir);
        Assert.assertEquals("Created target directory is not Folder type", targetDir.getName().getType(), FileType.FOLDER);

        MockFile failDir = MockFileHolder.getInstance().getFile(moveAfterFailure);
        Assert.assertNotNull("Fail to create expected directory to move files when failure", failDir);

        MockFileHolder.getInstance().clear();

    }

    /**
     * Function to extract {@link VFSTransportListener} object resides as private field withing
     * {@link TransportInDescription} object
     * @param trpInDescription target {@link TransportInDescription} object
     * @return listener object
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private VFSTransportListener getListener(TransportInDescription trpInDescription)
            throws NoSuchFieldException, IllegalAccessException {
        Field listenerField = TransportInDescription.class.getDeclaredField("receiver");
        listenerField.setAccessible(true);

        return (VFSTransportListener) listenerField.get(trpInDescription);
    }

    /**
     * Function to  populate private fields of given {@link PollTableEntry}
     * @param pollTableEntry target {@link PollTableEntry} instance
     * @param axisService  {@link AxisService}
     * @param vfsTransportListener {@link VFSTransportListener}
     */
    private void populatePollTableEntry(PollTableEntry pollTableEntry, AxisService axisService,
            VFSTransportListener vfsTransportListener) {
        try {
            Field axisServiceField = ProtocolEndpoint.class.getDeclaredField("service");
            axisServiceField.setAccessible(true);
            axisServiceField.set(pollTableEntry, axisService);

            Field listenerField = ProtocolEndpoint.class.getDeclaredField("listener");
            listenerField.setAccessible(true);
            listenerField.set(pollTableEntry, vfsTransportListener);
            
        } catch (NoSuchFieldException e) {
            log.error("Field does not exists", e);
        } catch (IllegalAccessException e) {
            log.error("Error occurred while setting axis service", e);
        }
    }

}
