package com.thundertech;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

public class DelayedShowsWorkerThreadsJobTime {
    public static void main(String[] args) throws InterruptedException {
        System.out.printf("Main thread --- %s --- START --- %n", Thread.currentThread().getName());
        var start = Instant.now();
        
        for(int i = 0; i < 1000000; i++) {
            Mono.just(i)
                    .delayElement(Duration.ofMillis(1000))
                    .subscribe(index -> {
                        System.out.printf("Total threads count: %s %n", Thread.activeCount());
                        System.out.printf("Mono %s handeled by - %s %n", index, Thread.currentThread().getName());
                    });
        }


        var finish = Instant.now();
        Thread.sleep(5000);
        System.out.printf("Main thread --- %s --- END --- %n", Thread.currentThread().getName());
        System.out.printf("Finished time: %s %n", Duration.between(start, finish).toMillis());
    }
}