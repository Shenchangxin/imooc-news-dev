package com.imooc.search.controller;

import com.imooc.api.controller.user.HelloControllerApi;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.search.pojo.Stu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HelloController implements HelloControllerApi {

    final static Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Autowired
    private ElasticsearchTemplate esTemplate;

    public Object hello(){

        return GraceJSONResult.ok("helloWorld");
    }

    @GetMapping("createIndex")
    public Object createIndex(){

        esTemplate.createIndex(Stu.class);
        return GraceJSONResult.ok();
    }

    @GetMapping("deleteIndex")
    public Object deleteIndex(){

        esTemplate.deleteIndex(Stu.class);
        return GraceJSONResult.ok();
    }


}
