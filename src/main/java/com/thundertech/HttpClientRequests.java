package com.thundertech;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.function.Function;

public class HttpClientRequests {

    public static void main(String[] args) {
//        runRequests(5000, new FluxRequests());
//        runRequests(5000, new VirtualThreadPerRequest());
        runRequests(1000, new ThreadPerRequest());
    }

    private static void runRequests(int requestsCount, Requests requests) {
        Function<Integer, HttpRequest> createRequest = i -> {
            try {
                return HttpRequest.newBuilder()
//                        .uri(new URI("https://jsonplaceholder.typicode.com/todos/" + i))
                        .uri(new URI("http://127.0.0.1:8088/status"))
                        .GET()
                        .build();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        };

        var workersNames = new ConcurrentHashMap<String, Integer>();

        System.out.println("----------------- START -----------------");
        System.out.printf("----------- REQUESTS WITH %s ----------- %n", requests.description());

        var start  = Instant.now();

        var client = HttpClient.newBuilder().build();

        requests.request(requestsCount, client, createRequest, workersNames);

        var finish = Instant.now();

        System.out.println("Threads workers size: " + workersNames.size());
        System.out.printf("Finished time: %s ms %n", Duration.between(start, finish).toMillis());
        System.out.println("----------------- END -----------------");
    }

    private record FluxRequests() implements Requests {

        @Override
        public String description() {
            return "FLUX";
        }

        @Override
        public void request(Integer requestsCount, HttpClient client,
                            Function<Integer, HttpRequest> createRequest,
                            ConcurrentHashMap<String, Integer> workers) {

            Flux.range(0, requestsCount)
                    .flatMap(i ->
                            Mono.fromFuture(
                                    client.sendAsync(createRequest.apply(i), HttpResponse.BodyHandlers.ofString())
                            )
                            .map(resp -> {
                                workers.put(Thread.currentThread().getName(), 1);
                                return resp;
                            })
                    )
                    .collectList()
                    // block cause need to wait all fluxes done
                    // before main thread finishes
                    .block();

        }
    }

    private record ThreadPerRequest() implements Requests {

        @Override
        public String description() {
                return "THREAD PER REQUEST";
            }

        @Override
        public void request(Integer requestsCount, HttpClient client,
                            Function<Integer, HttpRequest> createRequest,
                            ConcurrentHashMap<String, Integer> workers) {
            try(var executor = Executors.newThreadPerTaskExecutor(Executors.defaultThreadFactory())) {
                for (int i = 0; i < requestsCount; i++) {
                    int index = i;
                    executor.submit(() -> {
                        try {
                            client.send(createRequest.apply(index), HttpResponse.BodyHandlers.ofString());
                            workers.put(Thread.currentThread().getName(), 1);
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }

    private record VirtualThreadPerRequest() implements Requests {

        @Override
        public String description() {
            return "VIRTUAL THREAD PER REQUEST";
        }

        @Override
        public void request(Integer requestsCount, HttpClient client,
                            Function<Integer, HttpRequest> createRequest,
                            ConcurrentHashMap<String, Integer> workers) {
            try(var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < requestsCount; i++) {
                    int index = i;
                    executor.execute(() -> {
                        try {
                            client.send(createRequest.apply(index), HttpResponse.BodyHandlers.ofString());
                            workers.put("Virtual thread id: " + Thread.currentThread().threadId(), 1);
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }

    interface Requests {

        String description();

        void request(Integer requestsCount, HttpClient client,
                     Function<Integer, HttpRequest> createRequest,
                     ConcurrentHashMap<String, Integer> workers);

    }
}