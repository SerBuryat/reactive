package com.thundertech.reactivetgbot;

import java.io.IOException;

public class ReactiveTgBotPractice {

    private static final String TOKEN =
            "7316336271:AAG0NaaWgICS5HJgs6CGXOGr_DiWxm7gcTM";

    public static void main(String[] args) throws InterruptedException, IOException {
        var bot = ReactiveTgBot.builder(TOKEN).build();

//        var getMe = bot.getMe().block();
//        System.out.println(getMe.toPrettyString());
//
//        var getUpdates = bot.getUpdates().block();
//        System.out.println(getUpdates.toPrettyString());
//
//        bot.getUpdates(Duration.ofSeconds(5))
//                .subscribe(resp -> System.out.println(resp.toPrettyString()));
//
//        Thread.sleep(30_000);

        var chatId = "-4521603455";
//        var respSendMessage = bot.sendMessage(chatId, "Hello form reactive Netty!").block();
        var respSendFile = bot.sendDocument(
                chatId,
                "file sending title",
                "src/main/resources/send-to-tg-bot.json"
        ).block();

//        System.out.println(respSendMessage);
        System.out.println(respSendFile);
    }

}
