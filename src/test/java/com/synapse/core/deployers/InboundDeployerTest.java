package com.synapse.core.deployers;

import com.synapse.core.artifacts.inbound.Inbound;
import com.synapse.core.artifacts.utils.Position;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;

import static org.junit.jupiter.api.Assertions.*;

class InboundDeployerTest {

    @Test
    void testUnmarshalValidInbound() throws XMLStreamException {
        String xmlData = """
            <inboundEndpoint xmlns="http://ws.apache.org/ns/synapse"
                             name="file" sequence="inboundSeq"
                             onError="fault" protocol="file" suspend="false">
                <parameters>
                    <parameter name="interval">5000</parameter>
                    <parameter name="sequential">true</parameter>
                </parameters>
            </inboundEndpoint>
        """;

        Position position = new Position(1, "fileInbound.xml", "root");
        InboundDeployer inboundDeployer = new InboundDeployer();
        Inbound inbound = inboundDeployer.unmarshal(xmlData, position);

        assertNotNull(inbound);
        assertEquals("file", inbound.getName());
        assertEquals("inboundSeq", inbound.getSequence());
        assertEquals("file", inbound.getProtocol());
        assertEquals("false", inbound.getSuspend());
        assertEquals(2, inbound.getParameters().size());
    }

    @Test
    void testUnmarshalInvalidInbound() throws XMLStreamException {
        String invalidXmlData = "<invalidElement></invalidElement>";

        Position position = new Position(1, "invalid.xml", "root");
        InboundDeployer inboundDeployer = new InboundDeployer();
        Inbound inbound = inboundDeployer.unmarshal(invalidXmlData, position);

        assertNull(inbound);
    }
}