package com.minio.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MinioStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinioStorageApplication.class, args);
    }
}
