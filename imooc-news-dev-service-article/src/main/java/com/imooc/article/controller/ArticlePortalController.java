package com.imooc.article.controller;

import com.imooc.api.BaseController;
import com.imooc.api.controller.article.ArticleControllerApi;
import com.imooc.api.controller.article.ArticlePortalControllerApi;
import com.imooc.api.controller.user.UserControllerApi;
import com.imooc.article.service.ArticlePortalService;
import com.imooc.article.service.ArticleService;
import com.imooc.enums.ArticleCoverType;
import com.imooc.enums.ArticleReviewStatus;
import com.imooc.enums.YesOrNo;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.pojo.Article;
import com.imooc.pojo.Category;
import com.imooc.pojo.bo.NewArticleBO;
import com.imooc.pojo.vo.AppUserVO;
import com.imooc.pojo.vo.ArticleDetailVO;
import com.imooc.pojo.vo.IndexArticleVO;
import com.imooc.utils.IPUtil;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import javax.servlet.http.HttpServletRequest;
import java.util.*;


@RestController
public class ArticlePortalController extends BaseController implements ArticlePortalControllerApi {

    final static Logger logger = LoggerFactory.getLogger(ArticlePortalController.class);


    @Autowired
    private ArticlePortalService articlePortalService;

    @Autowired
    private RestTemplate restTemplate;


    @Override
    public GraceJSONResult list(String keyword, Integer category, Integer page, Integer pageSize) {

        if (page == null){
            page = COMMON_START_PAGE;
        }
        if (pageSize == null){
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = articlePortalService.queryIndexArticleList(keyword,category,page,pageSize);
// START

//        List<Article> list = (List<Article>) gridResult.getRows();
//
//        //1.构建发布者id列表
//        Set<String> idSet = new HashSet<>();
//        for (Article a : list){
//            idSet.add(a.getPublishUserId());
//        }
//
//        System.out.println(idSet.toString());
//
//        //2.发起远程调用（restTemplate），请求用户服务获得用户（idSet发布者）的列表
//        String userServerUrlExecute = "http://user.imoocnews.com:8003/user/queryByIds?userIds=" + JsonUtils.objectToJson(idSet);
//        ResponseEntity<GraceJSONResult> responseEntity = restTemplate.getForEntity(userServerUrlExecute,GraceJSONResult.class);
//        GraceJSONResult bodyResult = responseEntity.getBody();
//        List<AppUserVO> publisherList = null;
//        if (bodyResult.getStatus() == 200){
//            String userJson = JsonUtils.objectToJson(bodyResult.getData());
//            publisherList = JsonUtils.jsonToList(userJson,AppUserVO.class);
//        }
////        for (AppUserVO u : publisherList){
////            System.out.println(u.toString());
////        }
//
//        //3. 拼接两个list，重组文章列表
//        List<IndexArticleVO> indexArticleList = new ArrayList<>();
//        for (Article a : list){
//            IndexArticleVO indexArticleVO = new IndexArticleVO();
//            BeanUtils.copyProperties(a,indexArticleVO);
//
//            //3.1从publisherList中获得发布者的基本信息
//            AppUserVO publisher = getUserIfPublisher(a.getPublishUserId(),publisherList);
//            indexArticleVO.setPublisherVO(publisher);
//            indexArticleList.add(indexArticleVO);
//        }
//        gridResult.setRows(indexArticleList);


// END
        gridResult = rebuildArticleGrid(gridResult);

        return GraceJSONResult.ok(gridResult);
    }

    @Override
    public GraceJSONResult hotList() {

        return GraceJSONResult.ok(articlePortalService.queryHotList());
    }

    @Override
    public GraceJSONResult queryArticleListOfWriter(String writerId, Integer page, Integer pageSize) {

        System.out.println("writerId=" + writerId);

        if (page == null) {
            page = COMMON_START_PAGE;
        }

        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = articlePortalService.queryArticleListOfWriter(writerId, page, pageSize);
        gridResult = rebuildArticleGrid(gridResult);
        return GraceJSONResult.ok(gridResult);
    }

    @Override
    public GraceJSONResult queryGoodArticleListOfWriter(String writerId) {
        PagedGridResult gridResult = articlePortalService.queryGoodArticleListOfWriter(writerId);
        return GraceJSONResult.ok(gridResult);
    }

    @Override
    public GraceJSONResult detail(String articleId) {
        ArticleDetailVO detailVO = articlePortalService.queryDetail(articleId);

        Set<String> idSet = new HashSet();
        idSet.add(detailVO.getPublishUserId());
        List<AppUserVO> publisherList = getPublisherList(idSet);

        if (!publisherList.isEmpty()) {
            detailVO.setPublishUserName(publisherList.get(0).getNickname());
        }

        detailVO.setReadCounts(
                getCountsFromRedis(REDIS_ARTICLE_READ_COUNTS + ":" + articleId));

        return GraceJSONResult.ok(detailVO);
    }

    @Override
    public Integer readCounts(String articleId) {

        return getCountsFromRedis(REDIS_ARTICLE_READ_COUNTS + ":" + articleId);
    }

    @Override
    public GraceJSONResult readArticle(String articleId, HttpServletRequest request) {

        String userIp = IPUtil.getRequestIp(request);
        //设置针对当前用户ip的永久存在的key，存入到redis，表示该ip用户已经阅读过了，无法增加阅读量
        redis.setnx(REDIS_ALREADY_READ + ":" + articleId + ":" + userIp,userIp);

        redis.increment(REDIS_ARTICLE_READ_COUNTS + ":" + articleId,1);

        return GraceJSONResult.ok();
    }

    private AppUserVO getUserIfPublisher(String publisherId,List<AppUserVO> publisherList){

        for (AppUserVO user : publisherList){
            if (user.getId().equalsIgnoreCase(publisherId)){
                return user;
            }
        }
        return null;

    }

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private UserControllerApi userControllerApi;
    // 发起远程调用，获得用户的基本信息
    private List<AppUserVO> getPublisherList(Set idSet) {

//        String serviceId = "SERVICE-USER";
//        List<ServiceInstance> instanceList = discoveryClient.getInstances(serviceId);
//        ServiceInstance userService = instanceList.get(0);

//        String userServerUrlExecute
//                = "http://" + userService.getHost() + ":" + userService.getPort()+"/user/queryByIds?userIds=" + JsonUtils.objectToJson(idSet);

//        String userServerUrlExecute
//                = "http://" + serviceId + "/user/queryByIds?userIds=" + JsonUtils.objectToJson(idSet);

        GraceJSONResult bodyResult = userControllerApi.queryByIds(JsonUtils.objectToJson(idSet));

//        String userServerUrlExecute
//                = "http://user.imoocnews.com:8003/user/queryByIds?userIds=" + JsonUtils.objectToJson(idSet);
//        ResponseEntity<GraceJSONResult> responseEntity
//                = restTemplate.getForEntity(userServerUrlExecute, GraceJSONResult.class);
//        GraceJSONResult bodyResult = responseEntity.getBody();
        List<AppUserVO> publisherList = null;
        if (bodyResult.getStatus() == 200) {
            String userJson = JsonUtils.objectToJson(bodyResult.getData());
            publisherList = JsonUtils.jsonToList(userJson, AppUserVO.class);
        }else {
            publisherList = new ArrayList<>();
        }
        return publisherList;
    }


    private PagedGridResult rebuildArticleGrid(PagedGridResult gridResult) {
        // START

        List<Article> list = (List<Article>)gridResult.getRows();

        // 1. 构建发布者id列表
        Set<String> idSet = new HashSet<>();
        List<String> idList = new ArrayList<>();
        for (Article a : list) {
//            System.out.println(a.getPublishUserId());
            // 1.1 构建发布者的set
            idSet.add(a.getPublishUserId());
            // 1.2 构建文章id的list
            idList.add(REDIS_ARTICLE_READ_COUNTS + ":" + a.getId());
        }
        System.out.println(idSet.toString());
        // 发起redis的mget批量查询api，获得对应的值
        List<String> readCountsRedisList = redis.mget(idList);

        // 2. 发起远程调用（restTemplate），请求用户服务获得用户（idSet 发布者）的列表
//        String userServerUrlExecute
//                = "http://user.imoocnews.com:8003/user/queryByIds?userIds=" + JsonUtils.objectToJson(idSet);
//        ResponseEntity<GraceJSONResult> responseEntity
//                = restTemplate.getForEntity(userServerUrlExecute, GraceJSONResult.class);
//        GraceJSONResult bodyResult = responseEntity.getBody();
//        List<AppUserVO> publisherList = null;
//        if (bodyResult.getStatus() == 200) {
//            String userJson = JsonUtils.objectToJson(bodyResult.getData());
//            publisherList = JsonUtils.jsonToList(userJson, AppUserVO.class);
//        }
        List<AppUserVO> publisherList = getPublisherList(idSet);
//        for (AppUserVO u : publisherList) {
//            System.out.println(u.toString());
//        }

        // 3. 拼接两个list，重组文章列表
        List<IndexArticleVO> indexArticleList = new ArrayList<>();
//        for (Article a : list) {
//            IndexArticleVO indexArticleVO = new IndexArticleVO();
//            BeanUtils.copyProperties(a, indexArticleVO);
//
//            // 3.1 从publisherList中获得发布者的基本信息
//            AppUserVO publisher  = getUserIfPublisher(a.getPublishUserId(), publisherList);
//            indexArticleVO.setPublisherVO(publisher);
//
//            // 3.2 重新组装设置文章列表中的阅读量
//            int readCounts = getCountsFromRedis(REDIS_ARTICLE_READ_COUNTS + ":" + a.getId());
//            indexArticleVO.setReadCounts(readCounts);
//
//            indexArticleList.add(indexArticleVO);
//        }
        for (int i = 0 ; i < list.size() ; i ++) {
            IndexArticleVO indexArticleVO = new IndexArticleVO();
            Article a = list.get(i);
            BeanUtils.copyProperties(a, indexArticleVO);

            // 3.1 从publisherList中获得发布者的基本信息
            AppUserVO publisher  = getUserIfPublisher(a.getPublishUserId(), publisherList);
            indexArticleVO.setPublisherVO(publisher);

            // 3.2 重新组装设置文章列表中的阅读量
            String redisCountsStr = readCountsRedisList.get(i);
            int readCounts = 0;
            if (StringUtils.isNotBlank(redisCountsStr)) {
                readCounts = Integer.valueOf(redisCountsStr);
            }
            indexArticleVO.setReadCounts(readCounts);

            indexArticleList.add(indexArticleVO);
        }


        gridResult.setRows(indexArticleList);
// END
        return gridResult;
    }


}
