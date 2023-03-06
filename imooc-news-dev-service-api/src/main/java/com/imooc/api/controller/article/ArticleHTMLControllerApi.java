package com.imooc.api.controller.article;

import com.imooc.grace.result.GraceJSONResult;
import com.imooc.pojo.bo.NewArticleBO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;

@Api(value = "静态化文章业务的controller", tags = {"静态化文章业务的controller"})
@RequestMapping("article/html")
public interface ArticleHTMLControllerApi {

    @GetMapping("download")
    @ApiOperation(value = "下载html", notes = "下载html", httpMethod = "GET")
    public Integer download(@RequestParam String articleId, @RequestParam String articleMongoId)throws Exception;

    @GetMapping("delete")
    @ApiOperation(value = "删除html", notes = "删除html", httpMethod = "GET")
    public Integer delete(String articleId) throws Exception;

}
