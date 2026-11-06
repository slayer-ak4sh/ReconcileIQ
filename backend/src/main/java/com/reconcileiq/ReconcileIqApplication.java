package com.reconcileiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ReconcileIqApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReconcileIqApplication.class, args);
    }
}
