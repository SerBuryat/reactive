package com.thundertech.tgnotifier;

public class ReactiveTgNotifierRunner {

    private static final String NOTIFICATION_CHAT_ID = "-4521603455";
    private static final ReactiveTgNotifier TG_NOTIFIER = new ReactiveTgNotifier();

    public static void main(String[] args) {
        try {
            throw new IllegalArgumentException("some error when illegal argument in system");
        } catch (Exception ex) {
            var notification = new Notification(
                    "Error from api: /sendOrder", "error description message...", ex
            );
            var resp = TG_NOTIFIER.send(NOTIFICATION_CHAT_ID, notification).block();
            System.out.println(resp);
        }
    }

}