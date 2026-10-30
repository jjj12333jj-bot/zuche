package com.zuche;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 微租车平台启动类
 *
 * <p>说明：Mapper 使用 @Mapper 注解逐个声明，不做包级 @MapperScan，
 * 避免把 module 下的 Service 接口误注册为 Mapper。</p>
 */
@SpringBootApplication
@EnableScheduling
public class ZucheApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZucheApplication.class, args);
    }
}
