package com.imooc.api.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CloudConfig {

    public CloudConfig(){

    }

    /**
     * 基于OKHttp3的配置来配置RestTemplate
     */
    @Bean
    @LoadBalanced //默认的负载均衡算法：轮询
    public RestTemplate restTemplate(){
        return new RestTemplate(new OkHttp3ClientHttpRequestFactory());
    }


}
