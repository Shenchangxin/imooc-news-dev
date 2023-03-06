package com.imooc.article.controller;

import com.imooc.api.BaseController;
import com.imooc.api.config.RabbitMQConfig;
import com.imooc.api.controller.article.ArticleControllerApi;
import java.io.InputStream;
import com.imooc.article.service.ArticleService;
import com.imooc.enums.ArticleCoverType;
import com.imooc.enums.ArticleReviewStatus;
import com.imooc.enums.YesOrNo;
import com.imooc.exception.GraceException;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.pojo.Category;
import com.imooc.pojo.bo.NewArticleBO;
import com.imooc.pojo.vo.AppUserVO;
import com.imooc.pojo.vo.ArticleDetailVO;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import com.imooc.utils.extend.AliTextReviewUtils;
import com.mongodb.client.gridfs.GridFSBucket;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.imooc.api.service.BaseService.REDIS_ALL_CATEGORY;

@RestController
public class ArticleController extends BaseController implements ArticleControllerApi {

    final static Logger logger = LoggerFactory.getLogger(ArticleController.class);


    @Autowired
    private ArticleService articleService;
    @Autowired
    private RestTemplate restTemplate;



    @Override
    public GraceJSONResult createArticle(@Valid NewArticleBO newArticleBO) {

        // 判断BindingResult是否保存错误的验证信息，如果有，则直接return
//        if (result.hasErrors()) {
//            Map<String, String> errorMap = getErrors(result);
//            return GraceJSONResult.errorMap(errorMap);
//        }

        // 判断文章封面类型，单图必填，纯文字则设置为空
        if (newArticleBO.getArticleType() == ArticleCoverType.ONE_IMAGE.type) {
            if (StringUtils.isBlank(newArticleBO.getArticleCover())) {
                return GraceJSONResult.errorCustom(ResponseStatusEnum.ARTICLE_COVER_NOT_EXIST_ERROR);
            }
        } else if (newArticleBO.getArticleType() == ArticleCoverType.WORDS.type) {
            newArticleBO.setArticleCover("");
        }

        // 判断分类id是否存在
        String allCatJson = redis.get(REDIS_ALL_CATEGORY);
        Category temp = null;
        if (StringUtils.isBlank(allCatJson)) {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.SYSTEM_OPERATION_ERROR);
        } else {
            List<Category> catList =
                    JsonUtils.jsonToList(allCatJson, Category.class);
            for (Category c : catList) {
                if(c.getId() == newArticleBO.getCategoryId()) {
                    temp = c;
                    break;
                }
            }
            if (temp == null) {
                return GraceJSONResult.errorCustom(ResponseStatusEnum.ARTICLE_CATEGORY_NOT_EXIST_ERROR);
            }
        }

        //System.out.println(newArticleBO.toString());

       articleService.createArticle(newArticleBO, temp);

        return GraceJSONResult.ok();
    }

    @Override
    public GraceJSONResult queryMyList(String userId, String keyword, Integer status, Date startDate, Date endDate, Integer page, Integer pageSize) {

        if (StringUtils.isBlank(userId)){
            return GraceJSONResult.errorCustom(ResponseStatusEnum.ARTICLE_QUERY_PARAMS_ERROR);

        }
        if (page == null){
            page = COMMON_START_PAGE;
        }
        if (pageSize == null){
            pageSize = COMMON_PAGE_SIZE;
        }

        //查询我的列表，调用service
        PagedGridResult grid = articleService.queryMyArticleList(userId,keyword,status,startDate,endDate,page,pageSize);

        return GraceJSONResult.ok(grid);
    }

    @Override
    public GraceJSONResult queryAllList(Integer status, Integer page, Integer pageSize) {

        if (page == null) {
            page = COMMON_START_PAGE;
        }

        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = articleService.queryAllArticleListAdmin(status, page, pageSize);

        return GraceJSONResult.ok(gridResult);
    }

    @Override
    public GraceJSONResult doReview(String articleId, Integer passOrNot) {

        Integer pendingStatus;
        if (passOrNot == YesOrNo.YES.type){
            //审核成功
            pendingStatus = ArticleReviewStatus.SUCCESS.type;
        }else if (passOrNot == YesOrNo.NO.type){
            //审核失败
            pendingStatus = ArticleReviewStatus.FAILED.type;

        }else {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.ARTICLE_REVIEW_ERROR);
        }
        //保存到数据库，更改文章的状态为审核成功或者失败
        articleService.updateArticleStatus(articleId,pendingStatus);
        if (pendingStatus == ArticleReviewStatus.SUCCESS.type){
            //审核成功，生成文章详情页静态页面
            try{
                //createArticleHTML(articleId);
                String articleMongoId = createArticleHTMLToGridFS(articleId);
                //存储到对应的文章，进行关联保存
                articleService.updateArticleToGridFS(articleId,articleMongoId);
                //调用消费端，执行下载html
//                doDownloadArticleHTML(articleId,articleMongoId);
                //发送消息到MQ队列,让消费端监听并且执行下载html
                doDownloadArticleHTMLByMQ(articleId,articleMongoId);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return GraceJSONResult.ok();
    }

    private void doDownloadArticleHTML(String articleId,String articleMongoId){
        String url = "http://localhost:8002/article/html/download?articleId=" + articleId + "&articleMongoId=" + articleMongoId;

        ResponseEntity<Integer> responseEntity = restTemplate.getForEntity(url,Integer.class);
        int status = responseEntity.getBody();
        if (status != HttpStatus.OK.value()){
            GraceException.display(ResponseStatusEnum.ARTICLE_REVIEW_ERROR);
        }


    }

    @Autowired
    private RabbitTemplate rabbitTemplate;
    private void doDownloadArticleHTMLByMQ(String articleId,String articleMongoId){

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_ARTICLE,
                "article.download.do",
                articleId + "," + articleMongoId
        );


    }

    @Value("${freemarker.html.article}")
    private String articlePath;

    // 文章生成HTML
    public void createArticleHTML(String articleId) throws Exception {

        Configuration cfg = new Configuration(Configuration.getVersion());
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates");

        Template template = cfg.getTemplate("detail.ftl", "utf-8");

        // 获得文章的详情数据
        ArticleDetailVO detailVO = getArticleDetail(articleId);
        Map<String, Object> map = new HashMap<>();
        map.put("articleDetail", detailVO);

        File tempDic = new File(articlePath);
        if (!tempDic.exists()) {
            tempDic.mkdirs();
        }

        String path = articlePath + File.separator + detailVO.getId() + ".html";

        Writer out = new FileWriter(path);
        template.process(map, out);
        out.close();
    }

    @Autowired
    private GridFSBucket gridFSBucket;

    public String createArticleHTMLToGridFS(String articleId) throws Exception {

        Configuration cfg = new Configuration(Configuration.getVersion());
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates");

        Template template = cfg.getTemplate("detail.ftl", "utf-8");

        // 获得文章的详情数据
        ArticleDetailVO detailVO = getArticleDetail(articleId);
        Map<String, Object> map = new HashMap<>();
        map.put("articleDetail", detailVO);

        String htmlContent = FreeMarkerTemplateUtils.processTemplateIntoString(template,map);
        //System.out.println(htmlContent);

        InputStream inputStream = IOUtils.toInputStream(htmlContent);
        ObjectId fileId = gridFSBucket.uploadFromStream(detailVO.getId() + ".html",inputStream);
        return fileId.toString();
    }

    //发起远程调用rest，获得文章详情数据
    public ArticleDetailVO getArticleDetail(String articleId){
        String url = "http://localhost:8001/portal/article/detail?articleId=" + articleId;
        ResponseEntity<GraceJSONResult> responseEntity = restTemplate.getForEntity(url,GraceJSONResult.class);
        GraceJSONResult bodyResult = responseEntity.getBody();
        ArticleDetailVO detailVO = null;
        if (bodyResult.getStatus() == 200){
            String detailJson = JsonUtils.objectToJson(bodyResult.getData());
            detailVO = JsonUtils.jsonToPojo(detailJson,ArticleDetailVO.class);

        }
        return detailVO;


    }

    @Override
    public GraceJSONResult delete(String userId, String articleId) {
        articleService.deleteArticle(userId, articleId);
        return GraceJSONResult.ok();
    }

    @Override
    public GraceJSONResult withdraw(String userId, String articleId) {
        articleService.withdrawArticle(userId, articleId);
        return GraceJSONResult.ok();
    }

}
