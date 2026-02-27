package com.demo.minidoamp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.demo.minidoamp.*.mapper")
public class MiniDoampApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniDoampApplication.class, args);
    }
}
