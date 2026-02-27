package com.demo.minidoamp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.demo.minidoamp.*.mapper")
@EnableScheduling
public class MiniDoampApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniDoampApplication.class, args);
    }
}
