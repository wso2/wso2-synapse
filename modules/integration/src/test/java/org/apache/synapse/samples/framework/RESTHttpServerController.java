/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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

package org.apache.synapse.samples.framework;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.config.SampleConfigConstants;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.UriBuilder;

/**
 * Responsible for programmatically starting up and shutting down
 * an HttpServer server instance in order to run a sample test.
 */
public class RESTHttpServerController extends AbstractBackEndServerController {

    private static final Log log = LogFactory.getLog(RESTHttpServerController.class);

    private static final String DEFAULT_PORT = "9000";

    private HttpServer webServer = null;

    private String packageName;

    private int port;

    public RESTHttpServerController(OMElement element) {

        super(element);

        packageName = SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_BE_SERVER_CONF_REST_PACKAGE_NAME, null);

        port = Integer.parseInt(SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_BE_SERVER_CONF_REST_HTTP_PORT, DEFAULT_PORT));

        ResourceConfig rc = new ResourceConfig().packages(packageName);
        try {
            webServer = GrizzlyHttpServerFactory.createHttpServer(getBaseURI(port), rc, false);
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public boolean startProcess() {

        log.info("Starting up HTTP server: " + serverName);
        try {
            if (!webServer.isStarted()) {
                webServer.start();
            }
            return true;
        } catch (IOException e) {
            log.error("Error while starting the http server", e);
        }
        return false;
    }

    @Override
    public boolean stopProcess() {

        log.info("Shutting down HTTP server: " + serverName);
        try {
            if (webServer.isStarted()) {
                webServer.shutdownNow();
            }
            return true;
        } catch (Exception e) {
            log.error("Error while shutting down the http server", e);
        }
        return false;
    }

    private static URI getBaseURI(int port) {

        return UriBuilder.fromUri("https://localhost/").port(port).build();
    }
}
