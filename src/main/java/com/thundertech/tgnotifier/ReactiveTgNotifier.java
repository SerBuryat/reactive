package com.thundertech.tgnotifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thundertech.reactivetgbot.ReactiveTgBot;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import reactor.core.publisher.Mono;

public class ReactiveTgNotifier {

    private static final String TOKEN =
            "7316336271:AAG0NaaWgICS5HJgs6CGXOGr_DiWxm7gcTM";

    private final ReactiveTgBot bot;
    private final ObjectMapper mapper;

    public ReactiveTgNotifier() {
        this.bot = ReactiveTgBot.builder(TOKEN).build();
        this.mapper = new ObjectMapper();
    }

    public Mono<JsonNode> send(String chatId, Notification ntf) {
        return Mono.just(
                new TgErrorNotification(
                        ntf.title(),
                        ntf.msg(),
                        OffsetDateTime.now().toLocalDateTime().toString(),
                        ntf.details()
                )
        )
        .map(this::createNotificationDetailsFile)
        .flatMap(file ->
                bot.sendDocument(
                        chatId,
                                ntf.title() + " \n " + ntf.msg(),
                                file.toString()
                        )
                // todo - delete temp file (blocking i/o)
                .doOnSuccess(resp -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }

    // todo - blocking i/o.
    //  Fix for reactive with nio or `publishOn()` for blocking calls in non-blocking context
    private Path createNotificationDetailsFile(TgErrorNotification tgErrorNotification) {
        File tmpDetailsFile;
        try {
            tmpDetailsFile = File.createTempFile(
                    "error-details",
                    ".json",
                    new File("src/main/resources/temp")
            );
            var json = mapper.writeValueAsString(tgErrorNotification);
            return Files.writeString(tmpDetailsFile.toPath(), json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    record TgErrorNotification(String name,
                               String msg,
                               String timestamp,
                               Object details) {

    }

}
