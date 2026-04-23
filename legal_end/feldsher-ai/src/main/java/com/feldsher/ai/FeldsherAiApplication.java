package com.feldsher.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.feldsher.ai.mapper")
public class FeldsherAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeldsherAiApplication.class, args);
    }
}
