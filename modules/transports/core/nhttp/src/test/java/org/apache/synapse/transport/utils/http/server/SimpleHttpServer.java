/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.transport.utils.http.server;

import org.apache.synapse.transport.utils.TCPUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple Http Server for Test http transport.
 */
public class SimpleHttpServer implements HttpServer {
    private Server server;
    private int port;

    public SimpleHttpServer() {
        this.port = selectAvailablePort();
        server = new Server(this.port);
        server.setHandler(new HelloHandler());
    }

    /**
     * Start the http server.
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        server.start();
    }

    /**
     * Stop the http server.
     *
     * @throws Exception
     */
    @Override
    public void stop() throws Exception {
        server.stop();
    }

    /**
     * Return the service url.
     *
     * @return service url.
     */
    public String getServerUrl() {
        return "http://localhost:" + this.port + "/hello";
    }

    /**
     * find a port which is not already open.
     *
     * @return a port number.
     */
    private int selectAvailablePort() {
        int port = new Random().nextInt(9999);
        port = Math.abs(port);
        if (TCPUtils.isPortOpen(port, "localhost")) {
            return selectAvailablePort();
        }
        return port;
    }

    /**
     * Handler class to set the response message
     */
    private class HelloHandler extends AbstractHandler {
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            response.setContentType("application/xml");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().print("<msg>hello</msg>");
        }
    }
}
