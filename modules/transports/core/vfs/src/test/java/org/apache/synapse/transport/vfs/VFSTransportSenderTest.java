package org.apache.synapse.transport.vfs;


import junit.framework.TestCase;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FilesCache;
import org.apache.commons.vfs2.cache.SoftRefFilesCache;
import org.apache.synapse.commons.vfs.VFSOutTransportInfo;

import java.lang.reflect.Field;
import java.util.Map;

public class VFSTransportSenderTest extends TestCase {

    private static final int FILE_SEND_ITERATIONS = 2;

    public void testMemoryLeakWhileLockCreation() throws AxisFault, NoSuchFieldException, IllegalAccessException {
        VFSTransportSender vfsTransportSender = new VFSTransportSender();

        ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());

        TransportOutDescription transportOutDescription = new TransportOutDescription("Test");

        vfsTransportSender.init(configurationContext, transportOutDescription);

        org.apache.axis2.context.MessageContext mc = new org.apache.axis2.context.MessageContext();

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

}