package com.hlrms.requestworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RequestWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            RequestWorkerApplication.class,
            args
        );
    }
}
