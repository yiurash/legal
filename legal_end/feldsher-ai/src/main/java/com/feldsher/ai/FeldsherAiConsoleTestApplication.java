package com.feldsher.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 控制台 AI 对话测试专用启动类。
 * 使用 console-test profile，且不启动 Web 容器，避免与服务日志混在一起。
 */
@SpringBootApplication
@MapperScan("com.feldsher.ai.mapper")
public class FeldsherAiConsoleTestApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(FeldsherAiConsoleTestApplication.class)
                .profiles("console-test")
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
