package com.thundertech.reactivetgbot;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.MULTIPART_FORM_DATA;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

public class ReactiveTgBot {

    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    ReactiveTgBot(HttpClient httpClient, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    @JsonInclude(Include.NON_NULL)
    record SendMessageBody(@JsonProperty("chat_id")
                           String chatId,
                           @JsonProperty("text")
                           String message
    ) {}

    @JsonInclude(Include.NON_NULL)
    record SendDocumentBody(@JsonProperty("chat_id")
                            String chatId,
                            @JsonProperty("document")
                            String document
    ) {}

    public static ReactiveTgBotBuilder builder(String token) {
        return new ReactiveTgBotBuilder(token);
    }

    public Mono<JsonNode> getMe() {
        return get("/getMe");
    }

    public Mono<JsonNode> sendMessage(String chatId, String message) {
        return post(
                "/sendMessage",
                headers -> headers.add(CONTENT_TYPE, APPLICATION_JSON),
                ByteBufFlux.fromString(
                        Mono.just(
                                mapper.valueToTree(
                                        new SendMessageBody(chatId, message)
                                ).toPrettyString()
                        )
                )
        );
    }

    public Mono<JsonNode> sendDocument(String chatId, String title, String filePath) {
        return postWithForm(
                "/sendDocument",
                headers -> headers.add(CONTENT_TYPE, MULTIPART_FORM_DATA),
                Map.of(
                        "chat_id", chatId,
                        "caption", title
                ),
                Map.of(
                        "document", new File(filePath)
                )
        );
    }

    /** Make <b>single</b> <code>/getUpdates</code> and return unconfirmed updates. */
    public Mono<JsonNode> getUpdates() {
        return get("/getUpdates");
    }

    /** Make <b>endless</b> <code>/getUpdates</code> with duration and return unconfirmed updates. */
    public Flux<JsonNode> getUpdates(Duration duration) {
        return Flux.interval(duration)
                .flatMap(l -> getUpdates());
    }

    /** For custom <code>GET</code> requests "application/json"*/
    public Mono<JsonNode> get(String uri) {
        return httpClient.get()
                .uri(uri)
                .responseContent()
                .aggregate()
                .asString()
                .handle((jsonString, sink) -> {
                    try {
                        sink.next(mapper.readTree(jsonString));
                    } catch (JsonProcessingException e) {
                        sink.error(new RuntimeException(e));
                    }
                });
    }
    /** For custom <code>POST/code> requests with "application/json" */
    public Mono<JsonNode> post(String uri,
                               Consumer<HttpHeaders> headers,
                               ByteBufFlux body) {
        return httpClient
                .headers(headers)
                .post()
                .uri(uri)
                .send(body)
                .responseContent()
                .aggregate()
                .asString()
                .handle((jsonString, sink) -> {
                    try {
                        sink.next(mapper.readTree(jsonString));
                    } catch (JsonProcessingException e) {
                        sink.error(new RuntimeException(e));
                    }
                });
    }


    /** For custom <code>POST/code> requests with "multipart/form-data" for files */
    public Mono<JsonNode> postWithForm(String uri,
                                       Consumer<HttpHeaders> headers,
                                       Map<String, String> formAttrs,
                                       Map<String, File> formFile) {
        return httpClient
                .headers(headers)
                .post()
                .uri(uri)
                .sendForm((req, form) -> {
                    form.multipart(true);
                    formAttrs.forEach(form::attr);
                    formFile.forEach(form::file);
                })
                .responseContent()
                .aggregate()
                .asString()
                .handle((jsonString, sink) -> {
                    try {
                        sink.next(mapper.readTree(jsonString));
                    } catch (JsonProcessingException e) {
                        sink.error(new RuntimeException(e));
                    }
                });
    }
}
