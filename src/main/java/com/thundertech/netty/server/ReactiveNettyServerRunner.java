package com.thundertech.netty.server;

public class ReactiveNettyServerRunner {

    public static void main(String[] args) {
        new ReactiveNettyServer().start(8081);
    }

}
