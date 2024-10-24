package com.thundertech.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class JettyServer {

    private final int port;

    private Server server;

    public JettyServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        server = new Server(port);

        var ctx = new ServletContextHandler();
        ctx.setContextPath("/");
        ctx.addServlet(StatusResponseServlet.class, "/status");
        ctx.addServlet(GoogleRedirectServlet.class, "/google");

        server.setHandler(ctx);

        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

}
