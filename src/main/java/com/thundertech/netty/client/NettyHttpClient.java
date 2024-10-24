package com.thundertech.netty.client;

import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

public class NettyHttpClient {

    private final HttpClient httpClient;

    public NettyHttpClient() {
        this.httpClient = HttpClient.create();
    }

    public Flux<String> get(String uri) {
        return httpClient.get()
                .uri(uri)
                .responseContent()
                .asString();
    }
}
