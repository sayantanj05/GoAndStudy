package com.goandstudybackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class GoAndStudyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoAndStudyBackendApplication.class, args);
    }
}
