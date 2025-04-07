package com.synapse.synapse;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.SimpleHTTPServer;

import java.nio.file.Paths;

public class Axis2Server {
    private static final int PORT = 8290;
    private static SimpleHTTPServer server;

    public static void start() {
        try {
            System.out.println("Starting Axis2 HTTP Server on port " + PORT);

            String repoPath = Paths.get("").toAbsolutePath().resolve("repository").toString();
            String axis2Xml = repoPath + "/conf/axis2.xml";

            System.out.println(axis2Xml);
            ConfigurationContext configContext = ConfigurationContextFactory
                    .createConfigurationContextFromFileSystem(axis2Xml);

            server = new SimpleHTTPServer(configContext, PORT);
            server.start();

            System.out.println("Axis2 HTTP Server started on port " + PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop();
            System.out.println("Axis2 HTTP Server stopped.");
        }
    }
}
