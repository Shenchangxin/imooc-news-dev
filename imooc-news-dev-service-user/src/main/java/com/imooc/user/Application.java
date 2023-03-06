package com.imooc.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@MapperScan(basePackages = "com.imooc.user.mapper")
@ComponentScan(basePackages = {"com.imooc","org.n3r.idworker"})
@SpringBootApplication
@EnableEurekaClient//开启eureka client，注册到server中
@EnableCircuitBreaker //开启hystrix熔断机制
public class Application {


    public static void main(String[] args) {
        SpringApplication.run(Application.class,args);
    }
}
