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
import com.imooc.pojo.eo.ArticleEO;
import com.imooc.pojo.vo.AppUserVO;
import com.imooc.pojo.vo.ArticleDetailVO;
import com.imooc.pojo.vo.IndexArticleVO;
import com.imooc.utils.IPUtil;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
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

    @Autowired
    private ElasticsearchTemplate esTemplate;


    @Override
    public GraceJSONResult eslist(String keyword, Integer category, Integer page, Integer pageSize) {

        /**
         * es?????????
         *      1.?????????????????????????????????
         *      2.????????????????????????
         *      3.?????????????????????
         */
        //es???????????????0?????????????????????????????????page??????-1
        if (page < 1) return null;
        page--;
        Pageable pageable = PageRequest.of(page,pageSize);
        AggregatedPage<ArticleEO> pagedArticle =null;

                SearchQuery query = null;
        //?????????????????????
        if (StringUtils.isBlank(keyword) && category == null){
            query = new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchAllQuery())
                    .withPageable(pageable)
                    .build();
            pagedArticle =esTemplate.queryForPage(query, ArticleEO.class);

        }
        //?????????????????????
        if (StringUtils.isBlank(keyword) && category != null){
            query = new NativeSearchQueryBuilder().withQuery(QueryBuilders.termQuery("categoryId",category))
                    .withPageable(pageable)
                    .build();
            pagedArticle =esTemplate.queryForPage(query, ArticleEO.class);
        }

        //?????????????????????
//        if (StringUtils.isNotBlank(keyword) && category == null){
//            query = new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchQuery("title",keyword))
//                    .withPageable(pageable)
//                    .build();
//        }

        //???????????????????????????????????????????????????
        String searchTitleField = "title";
        if (StringUtils.isNotBlank(keyword) && category == null){
            String preTag = "<font color='red'>";
            String postTag = "</font>";
            query = new NativeSearchQueryBuilder()
                    .withQuery(QueryBuilders.matchQuery(searchTitleField,keyword))
                    .withHighlightFields(new HighlightBuilder.Field(searchTitleField)
                            .preTags(preTag)
                            .postTags(postTag)
                    )
                    .withPageable(pageable)
                    .build();
            pagedArticle = esTemplate.queryForPage(query, ArticleEO.class, new SearchResultMapper() {
                @Override
                public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                    List<ArticleEO> articleHighLightList = new ArrayList<>();
                    SearchHits hits = response.getHits();
                    for (SearchHit h : hits) {
                        HighlightField highlightField = h.getHighlightFields().get(searchTitleField);
                        String title = highlightField.getFragments()[0].toString();

                        // ??????????????????????????????????????????????????????
                        String articleId = (String)h.getSourceAsMap().get("id");
                        Integer categoryId = (Integer)h.getSourceAsMap().get("categoryId");
                        Integer articleType = (Integer)h.getSourceAsMap().get("articleType");
                        String articleCover = (String)h.getSourceAsMap().get("articleCover");
                        String publishUserId = (String)h.getSourceAsMap().get("publishUserId");
                        Long dateLong = (Long)h.getSourceAsMap().get("publishTime");
                        Date publishTime = new Date(dateLong);

                        ArticleEO articleEO = new ArticleEO();
                        articleEO.setId(articleId);
                        articleEO.setCategoryId(categoryId);
                        articleEO.setTitle(title);
                        articleEO.setArticleType(articleType);
                        articleEO.setArticleCover(articleCover);
                        articleEO.setPublishUserId(publishUserId);
                        articleEO.setPublishTime(publishTime);

                        articleHighLightList.add(articleEO);
                    }

                    // ????????????es7??????????????????????????????????????????

                    return new AggregatedPageImpl<>((List<T>)articleHighLightList,
                            pageable,
                            response.getHits().totalHits);
                }

                @Override
                public <T> T mapSearchHit(SearchHit searchHit, Class<T> aClass) {
                    return null;
                }
            });

        }


        //??????????????????
//        AggregatedPage<ArticleEO> pagedArticle = esTemplate.queryForPage(query, ArticleEO.class);
        List<ArticleEO> articleEOList = pagedArticle.getContent();
        List<Article> articleList = new ArrayList<>();
        for (ArticleEO a : articleEOList){
//            System.out.println(a);
            Article article = new Article();
            BeanUtils.copyProperties(a,article);
            articleList.add(article);
        }

        //????????????????????????grid??????
        PagedGridResult gridResult = new PagedGridResult();
        gridResult.setRows(articleList);
        gridResult.setPage(page+1);
        gridResult.setTotal(pagedArticle.getTotalPages());
        gridResult.setRecords(pagedArticle.getTotalElements());

        gridResult = rebuildArticleGrid(gridResult);

        return GraceJSONResult.ok(gridResult);
    }

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
//        //1.???????????????id??????
//        Set<String> idSet = new HashSet<>();
//        for (Article a : list){
//            idSet.add(a.getPublishUserId());
//        }
//
//        System.out.println(idSet.toString());
//
//        //2.?????????????????????restTemplate???????????????????????????????????????idSet?????????????????????
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
//        //3. ????????????list?????????????????????
//        List<IndexArticleVO> indexArticleList = new ArrayList<>();
//        for (Article a : list){
//            IndexArticleVO indexArticleVO = new IndexArticleVO();
//            BeanUtils.copyProperties(a,indexArticleVO);
//
//            //3.1???publisherList?????????????????????????????????
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
        //????????????????????????ip??????????????????key????????????redis????????????ip????????????????????????????????????????????????
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
    // ????????????????????????????????????????????????
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

        // 1. ???????????????id??????
        Set<String> idSet = new HashSet<>();
        List<String> idList = new ArrayList<>();
        for (Article a : list) {
//            System.out.println(a.getPublishUserId());
            // 1.1 ??????????????????set
            idSet.add(a.getPublishUserId());
            // 1.2 ????????????id???list
            idList.add(REDIS_ARTICLE_READ_COUNTS + ":" + a.getId());
        }
        System.out.println(idSet.toString());
        // ??????redis???mget????????????api?????????????????????
        List<String> readCountsRedisList = redis.mget(idList);

        // 2. ?????????????????????restTemplate???????????????????????????????????????idSet ?????????????????????
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

        // 3. ????????????list?????????????????????
        List<IndexArticleVO> indexArticleList = new ArrayList<>();
//        for (Article a : list) {
//            IndexArticleVO indexArticleVO = new IndexArticleVO();
//            BeanUtils.copyProperties(a, indexArticleVO);
//
//            // 3.1 ???publisherList?????????????????????????????????
//            AppUserVO publisher  = getUserIfPublisher(a.getPublishUserId(), publisherList);
//            indexArticleVO.setPublisherVO(publisher);
//
//            // 3.2 ?????????????????????????????????????????????
//            int readCounts = getCountsFromRedis(REDIS_ARTICLE_READ_COUNTS + ":" + a.getId());
//            indexArticleVO.setReadCounts(readCounts);
//
//            indexArticleList.add(indexArticleVO);
//        }
        for (int i = 0 ; i < list.size() ; i ++) {
            IndexArticleVO indexArticleVO = new IndexArticleVO();
            Article a = list.get(i);
            BeanUtils.copyProperties(a, indexArticleVO);

            // 3.1 ???publisherList?????????????????????????????????
            AppUserVO publisher  = getUserIfPublisher(a.getPublishUserId(), publisherList);
            indexArticleVO.setPublisherVO(publisher);

            // 3.2 ?????????????????????????????????????????????
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
