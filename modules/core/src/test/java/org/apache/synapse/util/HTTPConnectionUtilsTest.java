package org.apache.synapse.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLStreamException;

/**
 * Unit tests for the HTTPConnectionUtils class.
 * This class contains various test cases to verify the correct generation of OMElements
 * for different HTTP endpoint configurations.
 */
public class HTTPConnectionUtilsTest {

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration.
     * This test verifies that the generated OMElement matches the expected structure
     * for a given HTTP connection configuration.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGeneration() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>https</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <certificateInputType>File</certificateInputType>\n" +
                        "    <authType>BasicCredentials</authType>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <trustStoreCertificatePath>resources:certificates/serverpubliccert.crt</trustStoreCertificatePath>\n" +
                        "    <suspendErrorCodes>403</suspendErrorCodes>\n" +
                        "    <suspendProgressionFactor>1</suspendProgressionFactor>\n" +
                        "    <retryErrorCodes>300</retryErrorCodes>\n" +
                        "    <retryCount>3</retryCount>\n" +
                        "    <retryDelay>20</retryDelay>\n" +
                        "    <timeoutAction>Discard</timeoutAction>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\">" +
                        "<timeout>" +
                        "<responseAction>Discard</responseAction>" +
                        "</timeout>" +
                        "<suspendOnFailure>" +
                        "<errorCodes>403</errorCodes>" +
                        "<progressionFactor>1</progressionFactor>" +
                        "</suspendOnFailure>" +
                        "<markForSuspension>" +
                        "<errorCodes>300</errorCodes>" +
                        "<retriesBeforeSuspension>3</retriesBeforeSuspension>" +
                        "<retryDelay>20</retryDelay>" +
                        "</markForSuspension>" +
                        "</http>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals("Generated OMElement does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration without error handling.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithoutErrorHandling() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>http</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\" />" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals("Generated OMElement without error handling properties does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with trace enabled.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithTrace() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>http</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <trace>enable</trace>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http trace=\"enable\" uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\" />" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals("Generated OMElement with trace enabled does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with statistics enabled.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithStatistics() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>http</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <statistics>enable</statistics>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http statistics=\"enable\" uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\" />" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals("Generated OMElement with statistics enabled does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with description.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithMiscellaneousDescription() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>http</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <statistics>enable</statistics>\n" +
                        "    <miscellaneousDescription>hello world</miscellaneousDescription>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http statistics=\"enable\" uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\" />" +
                        "<description>hello world</description>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals("Generated OMElement with description does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with miscellaneous properties.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithMiscellaneousProperties() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>http</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <statistics>enable</statistics>\n" +
                        "    <miscellaneousDescription>hello world</miscellaneousDescription>\n" +
                        "    <miscellaneousProperties>name1:scope1:value1,name2:scope2:value2,</miscellaneousProperties>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http statistics=\"enable\" uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\" />" +
                        "<property name=\"name1\" scope=\"scope1\" value=\"value1\" />" +
                        "<property name=\"name2\" scope=\"scope2\" value=\"value2\" />" +
                        "<description>hello world</description>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals("Generated OMElement with miscellaneous properties does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with
     * quality of service addressing properties.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithQualityOfServiceAddressing() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>http</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <statistics>enable</statistics>\n" +
                        "    <qualityServiceAddressOption>enable</qualityServiceAddressOption>\n" +
                        "    <qualityServiceAddressVersion>final</qualityServiceAddressVersion>\n" +
                        "    <qualityServiceAddressSeparateListener>enable</qualityServiceAddressSeparateListener>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http statistics=\"enable\" uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\">" +
                        "<enableAddressing separateListener=\"true\" version=\"final\" />" +
                        "</http>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals("Generated OMElement with quality of service addressing properties does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with
     * quality of service security policy.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithQualityOfServiceSecurityPolicy() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>http</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <statistics>enable</statistics>\n" +
                        "    <qualityServiceAddressOption>enable</qualityServiceAddressOption>\n" +
                        "    <qualityServiceAddressVersion>final</qualityServiceAddressVersion>\n" +
                        "    <qualityServiceAddressSeparateListener>enable</qualityServiceAddressSeparateListener>\n" +
                        "    <qualityServiceSecurityOption>enable</qualityServiceSecurityOption>\n" +
                        "    <qualityServiceSecurityPolicyKey>samplePolicy</qualityServiceSecurityPolicyKey>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http statistics=\"enable\" uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\">" +
                        "<enableAddressing separateListener=\"true\" version=\"final\" />" +
                        "<enableSec policy=\"samplePolicy\" />" +
                        "</http>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals("Generated OMElement with quality of service security policy does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with
     * quality of service security inbound outbound policy.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithQualityOfServiceSecurityInboundOutboundPolicy() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>http</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <statistics>enable</statistics>\n" +
                        "    <qualityServiceAddressOption>enable</qualityServiceAddressOption>\n" +
                        "    <qualityServiceAddressVersion>final</qualityServiceAddressVersion>\n" +
                        "    <qualityServiceAddressSeparateListener>enable</qualityServiceAddressSeparateListener>\n" +
                        "    <qualityServiceSecurityOption>enable</qualityServiceSecurityOption>\n" +
                        "    <qualityServiceSecurityInboundOutboundPolicyOption>enable</qualityServiceSecurityInboundOutboundPolicyOption>\n" +
                        "    <qualityServiceSecurityInboundPolicyKey>sampleInboundPolicy</qualityServiceSecurityInboundPolicyKey>\n" +
                        "    <qualityServiceSecurityOutboundPolicyKey>sampleOutboundPolicy</qualityServiceSecurityOutboundPolicyKey>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http statistics=\"enable\" uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\">" +
                        "<enableAddressing separateListener=\"true\" version=\"final\" />" +
                        "<enableSec inboundPolicy=\"sampleInboundPolicy\" outboundPolicy=\"sampleOutboundPolicy\" />" +
                        "</http>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals(
                "Generated OMElement with quality of service security inbound outbound policy does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with TimeoutAction as Never.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithTimeoutActionNever() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>https</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <certificateInputType>File</certificateInputType>\n" +
                        "    <authType>BasicCredentials</authType>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <trustStoreCertificatePath>resources:certificates/serverpubliccert.crt</trustStoreCertificatePath>\n" +
                        "    <suspendErrorCodes>403</suspendErrorCodes>\n" +
                        "    <suspendProgressionFactor>1</suspendProgressionFactor>\n" +
                        "    <retryErrorCodes>300</retryErrorCodes>\n" +
                        "    <retryCount>3</retryCount>\n" +
                        "    <retryDelay>20</retryDelay>\n" +
                        "    <timeoutAction>Never</timeoutAction>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\">" +
                        "<timeout />" +
                        "<suspendOnFailure>" +
                        "<errorCodes>403</errorCodes>" +
                        "<progressionFactor>1</progressionFactor>" +
                        "</suspendOnFailure>" +
                        "<markForSuspension>" +
                        "<errorCodes>300</errorCodes>" +
                        "<retriesBeforeSuspension>3</retriesBeforeSuspension>" +
                        "<retryDelay>20</retryDelay>" +
                        "</markForSuspension>" +
                        "</http>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals("Generated OMElement with TimeoutAction as Never does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with Basic Authentication.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithBasicAuth() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>https</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <certificateInputType>File</certificateInputType>\n" +
                        "    <authType>Basic Auth</authType>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <trustStoreCertificatePath>resources:certificates/serverpubliccert.crt</trustStoreCertificatePath>\n" +
                        "    <suspendErrorCodes>403</suspendErrorCodes>\n" +
                        "    <suspendProgressionFactor>1</suspendProgressionFactor>\n" +
                        "    <retryErrorCodes>300</retryErrorCodes>\n" +
                        "    <retryCount>3</retryCount>\n" +
                        "    <retryDelay>20</retryDelay>\n" +
                        "    <timeoutAction>Discard</timeoutAction>\n" +
                        "    <basicCredentialsUsername>admin</basicCredentialsUsername>\n" +
                        "    <basicCredentialsPassword>admin</basicCredentialsPassword>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\">" +
                        "<timeout>" +
                        "<responseAction>Discard</responseAction>" +
                        "</timeout>" +
                        "<suspendOnFailure>" +
                        "<errorCodes>403</errorCodes>" +
                        "<progressionFactor>1</progressionFactor>" +
                        "</suspendOnFailure>" +
                        "<markForSuspension>" +
                        "<errorCodes>300</errorCodes>" +
                        "<retriesBeforeSuspension>3</retriesBeforeSuspension>" +
                        "<retryDelay>20</retryDelay>" +
                        "</markForSuspension>" +
                        "<authentication>" +
                        "<basicAuth>" +
                        "<username>admin</username>" +
                        "<password>admin</password>" +
                        "</basicAuth>" +
                        "</authentication>" +
                        "</http>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals("Generated OMElement with Basic Authentication does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with OAuth Authorization Code grant type.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithOAuthAuthorizationCode() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>https</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <certificateInputType>File</certificateInputType>\n" +
                        "    <authType>OAuth</authType>\n" +
                        "    <oauthAuthorizationMode>Header</oauthAuthorizationMode>\n" +
                        "    <oauthGrantType>Authorization Code</oauthGrantType>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <trustStoreCertificatePath>resources:certificates/serverpubliccert.crt</trustStoreCertificatePath>\n" +
                        "    <suspendErrorCodes>403</suspendErrorCodes>\n" +
                        "    <suspendProgressionFactor>1</suspendProgressionFactor>\n" +
                        "    <retryErrorCodes>300</retryErrorCodes>\n" +
                        "    <retryCount>3</retryCount>\n" +
                        "    <retryDelay>20</retryDelay>\n" +
                        "    <timeoutAction>Discard</timeoutAction>\n" +
                        "    <oauthAuthorizationClientId>admin</oauthAuthorizationClientId>\n" +
                        "    <oauthAuthorizationClientSecret>admin</oauthAuthorizationClientSecret>\n" +
                        "    <oauthAuthorizationTokenUrl>admin</oauthAuthorizationTokenUrl>\n" +
                        "    <oauthAuthorizationRefreshToken>admin</oauthAuthorizationRefreshToken>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\">" +
                        "<timeout>" +
                        "<responseAction>Discard</responseAction>" +
                        "</timeout>" +
                        "<suspendOnFailure>" +
                        "<errorCodes>403</errorCodes>" +
                        "<progressionFactor>1</progressionFactor>" +
                        "</suspendOnFailure>" +
                        "<markForSuspension>" +
                        "<errorCodes>300</errorCodes>" +
                        "<retriesBeforeSuspension>3</retriesBeforeSuspension>" +
                        "<retryDelay>20</retryDelay>" +
                        "</markForSuspension>" +
                        "<authentication>" +
                        "<oauth>" +
                        "<authorizationCode>" +
                        "<refreshToken>admin</refreshToken>" +
                        "<clientId>admin</clientId>" +
                        "<clientSecret>admin</clientSecret>" +
                        "<tokenUrl>admin</tokenUrl>" +
                        "<authMode>Header</authMode>" +
                        "</authorizationCode>" +
                        "</oauth>" +
                        "</authentication>" +
                        "</http>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals(
                "Generated OMElement with OAuth Authorization Code grant type does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with OAuth Client Credentials grant type.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithOAuthAuthorizationCodeAdditionalProperties() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>https</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <certificateInputType>File</certificateInputType>\n" +
                        "    <authType>OAuth</authType>\n" +
                        "    <oauthAuthorizationMode>Header</oauthAuthorizationMode>\n" +
                        "    <oauthGrantType>Authorization Code</oauthGrantType>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <trustStoreCertificatePath>resources:certificates/serverpubliccert.crt</trustStoreCertificatePath>\n" +
                        "    <suspendErrorCodes>403</suspendErrorCodes>\n" +
                        "    <suspendProgressionFactor>1</suspendProgressionFactor>\n" +
                        "    <retryErrorCodes>300</retryErrorCodes>\n" +
                        "    <retryCount>3</retryCount>\n" +
                        "    <retryDelay>20</retryDelay>\n" +
                        "    <timeoutAction>Discard</timeoutAction>\n" +
                        "    <oauthAuthorizationClientId>admin</oauthAuthorizationClientId>\n" +
                        "    <oauthAuthorizationClientSecret>admin</oauthAuthorizationClientSecret>\n" +
                        "    <oauthAuthorizationTokenUrl>admin</oauthAuthorizationTokenUrl>\n" +
                        "    <oauthAuthorizationRefreshToken>admin</oauthAuthorizationRefreshToken>\n" +
                        "    <oauthAuthorizationAdditionalProperties>name1:value1, name2:value2</oauthAuthorizationAdditionalProperties>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\">" +
                        "<timeout>" +
                        "<responseAction>Discard</responseAction>" +
                        "</timeout>" +
                        "<suspendOnFailure>" +
                        "<errorCodes>403</errorCodes>" +
                        "<progressionFactor>1</progressionFactor>" +
                        "</suspendOnFailure>" +
                        "<markForSuspension>" +
                        "<errorCodes>300</errorCodes>" +
                        "<retriesBeforeSuspension>3</retriesBeforeSuspension>" +
                        "<retryDelay>20</retryDelay>" +
                        "</markForSuspension>" +
                        "<authentication>" +
                        "<oauth>" +
                        "<authorizationCode>" +
                        "<refreshToken>admin</refreshToken>" +
                        "<clientId>admin</clientId>" +
                        "<clientSecret>admin</clientSecret>" +
                        "<tokenUrl>admin</tokenUrl>" +
                        "<authMode>Header</authMode>" +
                        "<requestParameters>" +
                        "<parameter name=\"name1\">value1</parameter>" +
                        "<parameter name=\"name2\">value2</parameter>" +
                        "</requestParameters>" +
                        "</authorizationCode>" +
                        "</oauth>" +
                        "</authentication>" +
                        "</http>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals(
                "Generated OMElement with OAuth Authorization Code grant type and additional properties does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with OAuth Authorization Code grant type
     * including expressions.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithOAuthAuthorizationCodeAndExpressions() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>https</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <certificateInputType>File</certificateInputType>\n" +
                        "    <authType>OAuth</authType>\n" +
                        "    <oauthAuthorizationMode>Header</oauthAuthorizationMode>\n" +
                        "    <oauthGrantType>Authorization Code</oauthGrantType>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <trustStoreCertificatePath>resources:certificates/serverpubliccert.crt</trustStoreCertificatePath>\n" +
                        "    <suspendErrorCodes>403</suspendErrorCodes>\n" +
                        "    <suspendProgressionFactor>1</suspendProgressionFactor>\n" +
                        "    <retryErrorCodes>300</retryErrorCodes>\n" +
                        "    <retryCount>3</retryCount>\n" +
                        "    <retryDelay>20</retryDelay>\n" +
                        "    <timeoutAction>Discard</timeoutAction>\n" +
                        "    <oauthAuthorizationClientId>{$ctx:admin}</oauthAuthorizationClientId>\n" +
                        "    <oauthAuthorizationClientSecret>{$ctx:admin}</oauthAuthorizationClientSecret>\n" +
                        "    <oauthAuthorizationTokenUrl>{$ctx:admin}</oauthAuthorizationTokenUrl>\n" +
                        "    <oauthAuthorizationRefreshToken>{$ctx:admin}</oauthAuthorizationRefreshToken>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\">" +
                        "<timeout>" +
                        "<responseAction>Discard</responseAction>" +
                        "</timeout>" +
                        "<suspendOnFailure>" +
                        "<errorCodes>403</errorCodes>" +
                        "<progressionFactor>1</progressionFactor>" +
                        "</suspendOnFailure>" +
                        "<markForSuspension>" +
                        "<errorCodes>300</errorCodes>" +
                        "<retriesBeforeSuspension>3</retriesBeforeSuspension>" +
                        "<retryDelay>20</retryDelay>" +
                        "</markForSuspension>" +
                        "<authentication>" +
                        "<oauth>" +
                        "<authorizationCode>" +
                        "<refreshToken>{$ctx:admin}</refreshToken>" +
                        "<clientId>{$ctx:admin}</clientId>" +
                        "<clientSecret>{$ctx:admin}</clientSecret>" +
                        "<tokenUrl>{$ctx:admin}</tokenUrl>" +
                        "<authMode>Header</authMode>" +
                        "</authorizationCode>" +
                        "</oauth>" +
                        "</authentication>" +
                        "</http>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals(
                "Generated OMElement with OAuth Authorization Code grant type and expressions does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with OAuth Client Credentials grant type.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithOAuthClientCredentials() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>https</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <certificateInputType>File</certificateInputType>\n" +
                        "    <authType>OAuth</authType>\n" +
                        "    <oauthAuthorizationMode>Header</oauthAuthorizationMode>\n" +
                        "    <oauthGrantType>Client Credentials</oauthGrantType>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <trustStoreCertificatePath>resources:certificates/serverpubliccert.crt</trustStoreCertificatePath>\n" +
                        "    <suspendErrorCodes>403</suspendErrorCodes>\n" +
                        "    <suspendProgressionFactor>1</suspendProgressionFactor>\n" +
                        "    <retryErrorCodes>300</retryErrorCodes>\n" +
                        "    <retryCount>3</retryCount>\n" +
                        "    <retryDelay>20</retryDelay>\n" +
                        "    <timeoutAction>Discard</timeoutAction>\n" +
                        "    <oauthClientClientId>admin</oauthClientClientId>\n" +
                        "    <oauthClientClientSecret>admin</oauthClientClientSecret>\n" +
                        "    <oauthClientTokenUrl>admin</oauthClientTokenUrl>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\">" +
                        "<timeout>" +
                        "<responseAction>Discard</responseAction>" +
                        "</timeout>" +
                        "<suspendOnFailure>" +
                        "<errorCodes>403</errorCodes>" +
                        "<progressionFactor>1</progressionFactor>" +
                        "</suspendOnFailure>" +
                        "<markForSuspension>" +
                        "<errorCodes>300</errorCodes>" +
                        "<retriesBeforeSuspension>3</retriesBeforeSuspension>" +
                        "<retryDelay>20</retryDelay>" +
                        "</markForSuspension>" +
                        "<authentication>" +
                        "<oauth>" +
                        "<clientCredentials>" +
                        "<clientId>admin</clientId>" +
                        "<clientSecret>admin</clientSecret>" +
                        "<tokenUrl>admin</tokenUrl>" +
                        "<authMode>Header</authMode>" +
                        "</clientCredentials>" +
                        "</oauth>" +
                        "</authentication>" +
                        "</http>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals(
                "Generated OMElement with OAuth Client Credentials grant type does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }

    /**
     * Tests the generation of an OMElement for an HTTP endpoint configuration with OAuth Password Credentials grant type.
     *
     * @throws XMLStreamException if an error occurs while parsing the XML
     */
    @Test
    public void testEndpointOMElementGenerationWithOAuthPassword() throws XMLStreamException {

        String omElementString =
                "<http.init>\n" +
                        "    <connectionType>https</connectionType>\n" +
                        "    <name>library</name>\n" +
                        "    <certificateInputType>File</certificateInputType>\n" +
                        "    <authType>OAuth</authType>\n" +
                        "    <oauthAuthorizationMode>Header</oauthAuthorizationMode>\n" +
                        "    <oauthGrantType>Password</oauthGrantType>\n" +
                        "    <baseUrl>http://jsonplaceholder.typicode.com/posts</baseUrl>\n" +
                        "    <trustStoreCertificatePath>resources:certificates/serverpubliccert.crt</trustStoreCertificatePath>\n" +
                        "    <suspendErrorCodes>403</suspendErrorCodes>\n" +
                        "    <suspendProgressionFactor>1</suspendProgressionFactor>\n" +
                        "    <retryErrorCodes>300</retryErrorCodes>\n" +
                        "    <retryCount>3</retryCount>\n" +
                        "    <retryDelay>20</retryDelay>\n" +
                        "    <timeoutAction>Discard</timeoutAction>\n" +
                        "    <oauthPasswordClientId>admin</oauthPasswordClientId>\n" +
                        "    <oauthPasswordClientSecret>admin</oauthPasswordClientSecret>\n" +
                        "    <oauthPasswordTokenUrl>admin</oauthPasswordTokenUrl>\n" +
                        "    <oauthPasswordUsername>admin</oauthPasswordUsername>\n" +
                        "    <oauthPasswordPassword>admin</oauthPasswordPassword>\n" +
                        "</http.init>";
        String expectedEndpointOMElementString =
                "<endpoint xmlns=\"http://ws.apache.org/ns/synapse\" name=\"library_INTERNAL_ENDPOINT_REFERENCE\">" +
                        "<http uri-template=\"{uri.var.base}{+uri.var.path}{+uri.var.query}\">" +
                        "<timeout>" +
                        "<responseAction>Discard</responseAction>" +
                        "</timeout>" +
                        "<suspendOnFailure>" +
                        "<errorCodes>403</errorCodes>" +
                        "<progressionFactor>1</progressionFactor>" +
                        "</suspendOnFailure>" +
                        "<markForSuspension>" +
                        "<errorCodes>300</errorCodes>" +
                        "<retriesBeforeSuspension>3</retriesBeforeSuspension>" +
                        "<retryDelay>20</retryDelay>" +
                        "</markForSuspension>" +
                        "<authentication>" +
                        "<oauth>" +
                        "<passwordCredentials>" +
                        "<username>admin</username>" +
                        "<password>admin</password>" +
                        "<clientId>admin</clientId>" +
                        "<clientSecret>admin</clientSecret>" +
                        "<tokenUrl>admin</tokenUrl>" +
                        "<authMode>Header</authMode>" +
                        "</passwordCredentials>" +
                        "</oauth>" +
                        "</authentication>" +
                        "</http>" +
                        "</endpoint>";
        InputStream inputStream = new ByteArrayInputStream(omElementString.getBytes(StandardCharsets.UTF_8));
        OMElement documentElement =
                new StAXOMBuilder(StAXUtils.createXMLStreamReader(inputStream)).getDocumentElement();

        OMElement generatedEndpointOMElement = HTTPConnectionUtils.generateHTTPEndpointOMElement(documentElement);
        Assert.assertEquals(
                "Generated OMElement with OAuth Password Credentials grant type does not match the expected structure",
                expectedEndpointOMElementString, generatedEndpointOMElement.toString());
    }
}
