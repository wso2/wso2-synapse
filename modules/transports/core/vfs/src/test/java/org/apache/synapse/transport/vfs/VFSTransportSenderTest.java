package org.apache.synapse.transport.vfs;


import junit.framework.TestCase;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FilesCache;
import org.apache.commons.vfs2.cache.SoftRefFilesCache;
import org.apache.synapse.commons.vfs.VFSConstants;
import org.apache.synapse.commons.vfs.VFSOutTransportInfo;
import org.junit.Assert;
import org.wso2.carbon.inbound.endpoint.protocol.file.MockFileHolder;

import java.lang.reflect.Field;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

/**
 * Unit test for VFSTransportSender
 */
public class VFSTransportSenderTest extends TestCase {

    private static Log log = LogFactory.getLog(VFSTransportSender.class);
    private static final int FILE_SEND_ITERATIONS = 2;

    /**
     * Test Transport sender initialization and sending a file
     * @throws AxisFault
     */
    public void testTransportSenderInitAndSend() throws AxisFault, XMLStreamException {

        //Clear file holder
        MockFileHolder.getInstance().clear();

        VFSTransportSender vfsTransportSender = new VFSTransportSender();
        ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
        TransportOutDescription transportOutDescription = new TransportOutDescription("Test");

        //Enable Autolock release
        transportOutDescription.addParameter(new Parameter(VFSConstants.TRANSPORT_AUTO_LOCK_RELEASE, true));
        //Set Autolock release interval with default value
        transportOutDescription.addParameter(new Parameter(VFSConstants.TRANSPORT_AUTO_LOCK_RELEASE_INTERVAL, 20000));

        transportOutDescription.addParameter(new Parameter(VFSConstants.TRANSPORT_AUTO_LOCK_RELEASE_SAME_NODE, true));

        vfsTransportSender.init(configurationContext, transportOutDescription);

        //Create message context
        org.apache.axis2.context.MessageContext mc = new org.apache.axis2.context.MessageContext();
        populateMessageContext(mc);

        String filePath = "test1:///foo/bar/test-" + System.currentTimeMillis() + ".ack";
        String parameters = "?transport.vfs.MaxRetryCount=0&transport.vfs.ReconnectTimeout=1&"
                + "transport.vfs.SendFileSynchronously=true&transport.vfs.CreateFolder=true";
        String fURI = filePath + parameters;

        OutTransportInfo outTransportInfo = new VFSOutTransportInfo(fURI, true);

        vfsTransportSender.sendMessage(mc, fURI, outTransportInfo);

        MockFileHolder fileHolder = MockFileHolder.getInstance();
        Assert.assertNotNull("File creation failed", fileHolder.getFile(filePath));
    }

    /**
     * Test creation of reply file in target directory
     * @throws AxisFault
     */
    public void testReplyFileCreation() throws AxisFault {

        String replyFileName = "testFile.txt";

        //Clear file holder
        MockFileHolder.getInstance().clear();

        VFSTransportSender vfsTransportSender = new VFSTransportSender();
        ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
        TransportOutDescription transportOutDescription = new TransportOutDescription("Test");

        vfsTransportSender.init(configurationContext, transportOutDescription);

        //Create message context
        org.apache.axis2.context.MessageContext mc = new org.apache.axis2.context.MessageContext();
        populateMessageContext(mc);

        //Add transport.vfs.ReplyFileName axis service parameter
        mc.getAxisService().addParameter(new Parameter(VFSConstants.REPLY_FILE_NAME, replyFileName));

        String filePath = "test1:///foo/bar/test-" + System.currentTimeMillis() + "/DIR/";
        String parameters = "?transport.vfs.CreateFolder=true";
        String fURI = filePath + parameters;

        OutTransportInfo outTransportInfo = new VFSOutTransportInfo(fURI, true);

        vfsTransportSender.sendMessage(mc, fURI, outTransportInfo);

        Assert.assertNotNull(MockFileHolder.getInstance().getFile(filePath + replyFileName));

    }

    public void testMemoryLeakWhileLockCreation() throws AxisFault, NoSuchFieldException, IllegalAccessException {

        //Clear file holder
        MockFileHolder.getInstance().clear();

        VFSTransportSender vfsTransportSender = new VFSTransportSender();

        ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());

        TransportOutDescription transportOutDescription = new TransportOutDescription("Test");

        vfsTransportSender.init(configurationContext, transportOutDescription);

        org.apache.axis2.context.MessageContext mc = new org.apache.axis2.context.MessageContext();
        populateMessageContext(mc);

        for (int i = 0; i < FILE_SEND_ITERATIONS; i++) {
            String fName = "test1://foo/bar/test" + i + "-" + System.currentTimeMillis()
                    + ".ack?transport.vfs.MaxRetryCount=0&transport.vfs.ReconnectTimeout=1";
            OutTransportInfo outTransportInfo = new VFSOutTransportInfo(fName, true);
            try {
                vfsTransportSender.sendMessage(mc, fName, outTransportInfo);
            } catch (AxisFault fse) {
                //Ignore
            }
        }

        //Perform the GC
        System.gc();

        Map<?, ?> refReverseMap = getSoftReferenceMap(vfsTransportSender);
        assertEquals("If there is no memory leak, soft reference map size should be zero.", 0, refReverseMap.size());

    }

    private Map<?, ?> getSoftReferenceMap(VFSTransportSender vfsTransportSender)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = VFSTransportSender.class.getDeclaredField("fsManager");
        field.setAccessible(true);

        FileSystemManager fsm = (FileSystemManager) field.get(vfsTransportSender);
        FilesCache fileCache = fsm.getFilesCache();
        SoftRefFilesCache softRefFilesCache = (SoftRefFilesCache) fileCache;
        Field field1 = SoftRefFilesCache.class.getDeclaredField("refReverseMap");
        field1.setAccessible(true);
        return (Map<?, ?>) (Map) field1.get(softRefFilesCache);
    }

    /**
     * Function to populate message context
     * @param messageContext message context
     * @throws AxisFault
     */
    private void populateMessageContext(MessageContext messageContext) throws AxisFault {
        SOAPFactory fac = OMAbstractFactory.getSOAP11Factory();
        SOAPEnvelope env = fac.getDefaultEnvelope();

        OMElement payload = fac.createOMElement(BaseConstants.DEFAULT_TEXT_WRAPPER);
        env.getBody().addChild(payload);
        messageContext.setEnvelope(env);

        AxisService axisService = new AxisService();
        messageContext.setAxisService(axisService);
    }
}