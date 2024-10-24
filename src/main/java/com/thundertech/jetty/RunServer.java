package com.thundertech.jetty;

public class RunServer {

    public static void main(String[] args) throws Exception {
        new JettyServer(8081).start();
    }

}
