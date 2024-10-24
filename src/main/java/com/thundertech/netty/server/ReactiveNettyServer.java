package com.thundertech.netty.server;

import com.thundertech.netty.client.NettyHttpClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.logging.AccessLog;
import reactor.netty.http.server.logging.AccessLogFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_DISPOSITION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.*;

public class ReactiveNettyServer {

    private static final NettyHttpClient HTTP_CLIENT = new NettyHttpClient();

    public void start(int port) {
        var totalBytesTransfer = new AtomicLong(0);
        var server = HttpServer.create()
                .accessLog(true, accessLogHandler(totalBytesTransfer))
                .host("localhost")
                .port(port)
                .route(routes -> {
                    routes.get(
                            "/smile",
                            (req, resp) -> resp.sendString(
                                    Mono.just("{ \"answer\": \":)\"}")
                            )
                    );
                    routes.get(
                            "/events",
                            sseHandle()
                    );
                    // use FileChannel and transferTo() - zer-byte coping from file socket to network socket
                    // 500 requests ~ 13GB -> ~2GB RAM usage
                    routes.get(
                            "/big-with-send-file",
                            (req, resp) -> resp.sendFile(bigJson(totalBytesTransfer))
                    );
                    // use blocking readAllBytes() read call and keep all file bytes in memory
                    // 100 requests -> ~3GB -> ~4GB RAM usage
                    routes.get(
                            "/big-with-read-all-bytes",
                            (req, resp) -> resp.send(bigJsonWithReadAllBytes(totalBytesTransfer))
                    );
                    routes.get("/file", (req, resp) ->
                            resp.header(CONTENT_TYPE, APPLICATION_JSON)
                                    .header(CONTENT_DISPOSITION, ATTACHMENT)
                                    .header(FILENAME, "some-json.json")
                                    .send(
                                            ByteBufFlux.fromPath(
                                                    Paths.get("src/main/resources/big-response.json")
                                            )
                                    )
                    );
                    routes.get("/third-party-proxy", (req, resp) ->
                            resp.sendString(
                                    HTTP_CLIENT.get("https://jsonplaceholder.typicode.com/todos/1")
                            )
                    );
                })
                .bindNow();

        server.onDispose().block();
    }

    private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> sseHandle() {;
        return (request, response) ->
                response.sse()
                        .sendString(Flux.interval(Duration.ofSeconds(5))
                                .map(l ->
                                        String.format(
                                                "{ \"event\": \"hi\", \"timestamp\": \"%s\"}",
                                                LocalDateTime.now()
                                        )
                                )
                        );
    }

    private AccessLogFactory accessLogHandler(AtomicLong totalBytesTransfer) {
        return logArgs ->
                AccessLog.create(
                        "method={} | uri={} | duration={} totalBytesTransfer={}",
                        logArgs.method(), logArgs.uri(), logArgs.duration() + "ms", totalBytesTransfer
                );
    }

    private Path bigJson(AtomicLong totalBytesTransfer) {
        var file = Paths.get("src/main/resources/big-response.json");
        try {
            totalBytesTransfer.addAndGet(Files.size(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    private Mono<ByteBuf> bigJsonWithReadAllBytes(AtomicLong totalBytesTransfer) {
        var file = Paths.get("src/main/resources/big-response.json");
        try {
            var bytes = Files.readAllBytes(file);
            totalBytesTransfer.addAndGet(bytes.length);
            return Mono.just(Unpooled.wrappedBuffer(bytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
