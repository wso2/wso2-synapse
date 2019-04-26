## Sample Test Cases
Each request needs to be send to the unit testing server via a TCP transport (user defined port or default 9008 port). 

### Sample test case for sequence artifact

Client request via TCP:
```
<?xml version="1.0" encoding="utf-8" ?>
<synapse-unit-test>
    <artifacts>
        <!-- One or more xml files contains synapse configurations -->
        <test-artifact>
            <artifact>
                <sequence xmlns="http://ws.apache.org/ns/synapse" name="sequenceTest">
                    <log level="full" />
                    <enrich>
                        <source clone="true" type="inline">
                            <xsd:symbol xmlns:xsd="http://services.samples/xsd">SUN</xsd:symbol>
                        </source>
                        <target xmlns:ser="http://services.samples" xmlns:xsd="http://services.samples/xsd" action="child" type="custom" xpath="//ser:getQuote/ser:request" />
                    </enrich>
                    <log level="full" />
                </sequence>
            </artifact>
        </test-artifact>
    </artifacts>

    <test-cases>
        <test-case>
            <input>
                <payload>
                    <![CDATA[
                        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"   xmlns:ser="http://services.samples" xmlns:xsd="http://services.samples/xsd">
                           <soapenv:Header/>
                           <soapenv:Body>
                              <ser:getQuote>
                                 <ser:request>
                                    <xsd:symbol>WSO2</xsd:symbol>
                                 </ser:request>
                              </ser:getQuote>
                           </soapenv:Body>
                        </soapenv:Envelope>
                    ]]>
                </payload>
                <properties>
                    <property name="prop1" value="val1"/>
                    <property name="prop2" scope="axis2" value="val2"/>
                    <property name="prop3" scope="transport" value="val3"/>
                </properties>
            </input>

            <assertions>
                <!-- assert for payload-->
                <assertEquals>
                    <actual>$body</actual>
                    <expected>
                        <![CDATA[
                            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://services.samples/xsd" xmlns:ser="http://services.samples">
                               <soapenv:Header/>
                               <soapenv:Body>
                                  <ser:getQuote>
                                     <ser:request>
                                        <xsd:symbol>WSO2</xsd:symbol>
                                        <xsd:symbol>SUN</xsd:symbol>
                                     </ser:request>
                                  </ser:getQuote>
                               </soapenv:Body>
                            </soapenv:Envelope>
                        ]]>
                    </expected>
                    <message>Expected payload not found</message>
                </assertEquals>

                <assertNotNull>
                    <actual>$body</actual>
                    <message>Payload is not available</message>
                </assertNotNull>

                <!-- assert for properties-->
                <assertNotNull>
                    <actual>$ctx:prop1</actual>
                    <message>prop1 not found</message>
                </assertNotNull>

                <assertEquals>
                    <actual>$ctx:prop1</actual>
                    <expected>val1</expected>
                    <message>Expected property value not found</message>
                </assertEquals>

                <assertEquals>
                    <actual>$axis2:prop2</actual>
                    <expected>val2</expected>
                    <message>Expected property value not found</message>
                </assertEquals>

                <assertEquals>
                    <actual>$trp:prop3</actual>
                    <expected>val3</expected>
                    <message>Expected property value not found</message>
                </assertEquals>
            </assertions>
        </test-case>
    </test-cases>
</synapse-unit-test>
```

Response from unit testing server:
```
{'test-cases':'SUCCESS'}
```

### Sample test case for proxy artifact

Client request via TCP:
```
<?xml version="1.0" encoding="utf-8" ?>
<synapse-unit-test>
    <artifacts>
        <!-- One or more xml files contains synapse configurations -->
        <test-artifact>
            <artifact>
                  <proxy name="HelloWorld" startOnLoad="true" transports="http https" xmlns="http://ws.apache.org/ns/synapse">
                      <target>
                          <inSequence>
                              <payloadFactory media-type="json">
                                  <format>{"Hello":"World"}</format>
                                  <args/>
                              </payloadFactory>
                              <respond/>
                          </inSequence>
                          <outSequence/>
                          <faultSequence/>
                      </target>
                  </proxy>
            </artifact>
        </test-artifact>
    </artifacts>

    <test-cases>
        <!-- One or more test cases with inputs and assertions -->
        <test-case>
            <assertions>
                <assertEquals>
                    <actual>$body</actual>
                    <expected>{"Hello":"World"}</expected>
                    <message>Failed the body assertion</message>
                </assertEquals>

                <assertNotNull>
                    <actual>$body</actual>
                    <message>Failed the body assertion (not null)</message>
                </assertNotNull>
            </assertions>
        </test-case>
    </test-cases>
</synapse-unit-test>
```
Response from unit testing server:
```
{'test-cases':'SUCCESS'}
```
### Sample test case for API artifact

Client request via TCP:
```
<synapse-unit-test>
    <artifacts>
        <!-- One or more xml files contains synapse configurations -->
        <test-artifact>
            <artifact>
                <api xmlns="http://ws.apache.org/ns/synapse" name="apiTests3" context="/orders1e">
        			<resource methods="GET" url-mapping="/">
                        <inSequence>
                            <log level="full" />
                            <send>
                                <endpoint key="Hospital" />
                            </send>
                        </inSequence>
                        <outSequence>
                            <log level="full" />
                            <respond/>
                        </outSequence>
                    </resource>
                </api>
            </artifact>
        </test-artifact>
        
        <supportive-artifacts>
            <artifact>
                <endpoint name="Hospital" xmlns="http://ws.apache.org/ns/synapse">
                	<address uri="http://localhost:9091/hello/sayHello" />
                </endpoint>
            </artifact>
        </supportive-artifacts>
    </artifacts>

    <test-cases>
        <!-- One or more test cases with inputs and assertions -->
        <test-case>
            <input>
                <request-path>/</request-path>
                <request-method>GET</request-method>
            </input>
            <assertions>
                <assertEquals>
                    <actual>$body</actual>
                    <expected>
                        {"fname":"Peter","lname":"Stallone", "age":22,"address":{"line":"20 Palm Grove","city":"Colombo 03","country":"Sri Lanka"}}
                    </expected>
                    <message>Failed the body assertion</message>
                </assertEquals>

                <assertNotNull>
                    <actual>$body</actual>
                    <message>Failed the body assertion (not null)</message>
                </assertNotNull>
            </assertions>
        </test-case>
    </test-cases>

    <mock-services>
        <!-- One or more mock services with multiple resources -->
        <mock-service>
            <service-name>Hospital</service-name>
            <port>9190</port>
            <context>/hello/sayHello</context>
            <resources>
                <resource>
                    <sub-context>/</sub-context>
                    <method>GET</method>
                    <response>
                        <payload>
                            {"fname":"Peter", "lname":"Stallone", "age":22, "address":{"line":"20 Palm Grove", "city":"Colombo 03", "country":"Sri Lanka"}}                        
                        </payload>
                    </response>
                </resource>
            </resources>
        </mock-service>
    </mock-services>
</synapse-unit-test>
```

Response from unit testing server:
```
{'test-cases':'SUCCESS'}
```
