package com.thundertech.netty.client;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.WRITE;

public class NettyClientRunner {

    // plazma_alert_bot
    private static final String BOT_GET_UPDATES_URL =
            "https://api.telegram.org/bot7316336271:AAG0NaaWgICS5HJgs6CGXOGr_DiWxm7gcTM/getUpdates";

    public static void main(String[] args) throws InterruptedException {
        var nettyClient = new NettyHttpClient();
        var totalBytes = new AtomicLong(0);

        var savedFileNameStart = "src/main/resources/saved/saved-big-response-";
        var savedFileNameEnd = ".json";

        Flux.range(0, 10)
                .flatMap(l ->
                        Flux.zip(
                                nettyClient.get("http://127.0.0.1:8089/big-with-send-file"),
                                Mono.fromCallable(() -> {
                                    var savedFilePath = Path.of(savedFileNameStart + l + savedFileNameEnd);
                                    try {
                                        Files.createFile(savedFilePath);
                                        return FileChannel.open(savedFilePath, WRITE, APPEND);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }).cache()
                        )
                )
                .map(respAndFileChannel -> {
                    var resp = respAndFileChannel.getT1();
                    var fc = respAndFileChannel.getT2();

                    try {
                        fc.write(ByteBuffer.wrap(resp.getBytes()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return resp;
                })
                .subscribe(resp -> {
                    totalBytes.addAndGet(resp.getBytes().length);
                    System.out.printf("Handled %s bytes %n", resp.getBytes().length);
                });

        Thread.sleep(5_000);
        System.out.printf("Total bytes handled: %s", totalBytes);
    }

}
