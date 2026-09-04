package com.localsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LocalSyncApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocalSyncApplication.class, args);
    }
}
