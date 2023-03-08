package com.imooc.user.service.Impl;

import com.github.pagehelper.PageHelper;
import com.imooc.api.service.BaseService;
import com.imooc.enums.Sex;
import com.imooc.pojo.AppUser;
import com.imooc.pojo.Fans;
import com.imooc.pojo.eo.FansEO;
import com.imooc.pojo.vo.FansCountsVO;
import com.imooc.pojo.vo.RegionRatioVO;
import com.imooc.user.mapper.AppUserMapper;
import com.imooc.user.mapper.FansMapper;
import com.imooc.user.service.MyFanService;
import com.imooc.user.service.UserService;
import com.imooc.utils.PagedGridResult;
import com.imooc.utils.RedisOperator;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.n3r.idworker.Sid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class MyFanServiceImpl extends BaseService implements MyFanService {

    @Autowired
    private FansMapper fansMapper;

    @Autowired
    public RedisOperator redis;

    @Autowired
    private UserService userService;

    @Autowired
    private Sid sid;

    @Autowired
    private ElasticsearchTemplate esTemplate;



    @Override
    public boolean isMeFollowThisWriter(String writerId, String fanId) {
        Fans fan = new Fans();
        fan.setFanId(fanId);
        fan.setWriterId(writerId);

        int count = fansMapper.selectCount(fan);

        return count > 0 ? true : false;
    }

    @Transactional
    @Override
    public void follow(String writerId, String fanId) {
        // 获得粉丝用户的信息
        AppUser fanInfo = userService.getUser(fanId);

        String fanPkId = sid.nextShort();

        Fans fans = new Fans();
        fans.setId(fanPkId);
        fans.setFanId(fanId);
        fans.setWriterId(writerId);

        fans.setFace(fanInfo.getFace());
        fans.setFanNickname(fanInfo.getNickname());
        fans.setSex(fanInfo.getSex());
        fans.setProvince(fanInfo.getProvince());

        fansMapper.insert(fans);

        // redis 作家粉丝数累加
        redis.increment(REDIS_WRITER_FANS_COUNTS + ":" + writerId, 1);
        // redis 当前用户的（我的）关注数累加
        redis.increment(REDIS_MY_FOLLOW_COUNTS + ":" + fanId, 1);

        //保存粉丝关系到es中
        FansEO fansEO = new FansEO();
        BeanUtils.copyProperties(fans,fansEO);
        IndexQuery iq = new IndexQueryBuilder().withObject(fansEO).build();
        esTemplate.index(iq);
    }

    @Transactional
    @Override
    public void unfollow(String writerId, String fanId) {

        Fans fans = new Fans();
        fans.setWriterId(writerId);
        fans.setFanId(fanId);

        fansMapper.delete(fans);

        // redis 作家粉丝数累减
        redis.decrement(REDIS_WRITER_FANS_COUNTS + ":" + writerId, 1);
        // redis 当前用户的（我的）关注数累减
        redis.decrement(REDIS_MY_FOLLOW_COUNTS + ":" + fanId, 1);

        //删除es中的粉丝关系，DeleteQuery：根据条件删除
        DeleteQuery dq = new DeleteQuery();
        dq.setQuery(QueryBuilders.termQuery("writerId",writerId));
        dq.setQuery(QueryBuilders.termQuery("fanId",fanId));
        esTemplate.delete(dq,FansEO.class);
    }

    @Override
    public PagedGridResult queryMyFansList(String writerId, Integer page, Integer pageSize) {

        Fans fans = new Fans();
        fans.setWriterId(writerId);

        PageHelper.startPage(page,pageSize);
        List<Fans> list = fansMapper.select(fans);

        return setterPagedGrid(list,page);
    }

    @Override
    public PagedGridResult queryMyFansESList(String writerId, Integer page, Integer pageSize) {

        page--;
        Pageable pageable = PageRequest.of(page,pageSize);

        SearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.termQuery("writerId",writerId))
                .withPageable(pageable)
                .build();

        AggregatedPage<FansEO> pagedFans = esTemplate.queryForPage(query,FansEO.class);

        PagedGridResult gridResult = new PagedGridResult();
        gridResult.setRows(pagedFans.getContent());
        gridResult.setPage(page+1);
        gridResult.setTotal(pagedFans.getTotalPages());
        gridResult.setRecords(pagedFans.getTotalElements());

        return gridResult;
    }

    @Override
    public Integer queryFansCounts(String writerId, Sex sex) {
        Fans fans = new Fans();
        fans.setWriterId(writerId);
        fans.setSex(sex.type);

        Integer count = fansMapper.selectCount(fans);
        return count;
    }

    @Override
    public FansCountsVO queryFansESCounts(String writerId) {

        TermsAggregationBuilder aggregationBuilder = AggregationBuilders
                .terms("sex_counts")
                .field("sex");

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery("writerId",writerId))
                .addAggregation(aggregationBuilder)
                .build();

        Aggregations aggs = esTemplate.query(searchQuery, new ResultsExtractor<Aggregations>() {

            @Override
            public Aggregations extract(SearchResponse response) {
                return response.getAggregations();
            }
        });

        Map aggMap = aggs.asMap();
        LongTerms longTerms = (LongTerms)aggMap.get("sex_counts");
        List bucketList = longTerms.getBuckets();

        FansCountsVO fansCountsVO = new FansCountsVO();
        for (int i=0;i<bucketList.size();i++){
            LongTerms.Bucket bucket = (LongTerms.Bucket)bucketList.get(i);
            Long docCounts = bucket.getDocCount();
            Long key = (Long)bucket.getKey();

            if (key.intValue() == Sex.woman.type){
                fansCountsVO.setWomanCounts(docCounts.intValue());
            }else if (key.intValue() == Sex.man.type){
                fansCountsVO.setManCounts(docCounts.intValue());
            }
        }

        if (bucketList == null || bucketList.size()==0){
            fansCountsVO.setWomanCounts(0);
            fansCountsVO.setManCounts(0);
        }
        return fansCountsVO;
    }

    public static final String[] regions = {"北京", "天津", "上海", "重庆",
            "河北", "山西", "辽宁", "吉林", "黑龙江", "江苏", "浙江", "安徽", "福建", "江西", "山东",
            "河南", "湖北", "湖南", "广东", "海南", "四川", "贵州", "云南", "陕西", "甘肃", "青海", "台湾",
            "内蒙古", "广西", "西藏", "宁夏", "新疆",
            "香港", "澳门"};

    @Override
    public List<RegionRatioVO> queryRegionRatioCounts(String writerId) {

        Fans fans = new Fans();
        fans.setWriterId(writerId);
        List<RegionRatioVO> list = new ArrayList<>();

        for (String r : regions){
            fans.setProvince(r);
            Integer count = fansMapper.selectCount(fans);

            RegionRatioVO regionRatioVO = new RegionRatioVO();
            regionRatioVO.setName(r);
            regionRatioVO.setValue(count);
            list.add(regionRatioVO);
        }


        return list;
    }

    @Override
    public List<RegionRatioVO> queryRegionRatioESCounts(String writerId) {
        TermsAggregationBuilder aggregationBuilder = AggregationBuilders
                .terms("region_counts")
                .field("province");

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery("writerId",writerId))
                .addAggregation(aggregationBuilder)
                .build();

        Aggregations aggs = esTemplate.query(searchQuery, new ResultsExtractor<Aggregations>() {

            @Override
            public Aggregations extract(SearchResponse response) {
                return response.getAggregations();
            }
        });

        Map aggMap = aggs.asMap();
        StringTerms strTerms = (StringTerms)aggMap.get("region_counts");
        List bucketList = strTerms.getBuckets();

        List<RegionRatioVO> list = new ArrayList<>();
        for (int i=0;i<bucketList.size();i++){
            StringTerms.Bucket bucket = (StringTerms.Bucket)bucketList.get(i);
            Long docCounts = bucket.getDocCount();
            String key = (String)bucket.getKey();

            System.out.println(docCounts);
            System.out.println(key);

            RegionRatioVO regionRatioVO = new RegionRatioVO();
            regionRatioVO.setName(key);
            regionRatioVO.setValue(docCounts.intValue());
            list.add(regionRatioVO);
        }

        return list;
    }

    @Override
    public void forceUpdateFanInfo(String relationId, String fanId) {
        //1.根据fansId查询用户信息
        AppUser user = userService.getUser(fanId);

        //2.更新用户信息到db和es中
        Fans fans = new Fans();
        fans.setId(relationId);
        fans.setFace(user.getFace());
        fans.setFanNickname(user.getNickname());
        fans.setSex(user.getSex());
        fans.setProvince(user.getProvince());

        fansMapper.updateByPrimaryKeySelective(fans);

        //3.更新到es中
        Map<String,Object> updateMap = new HashMap<>();
        updateMap.put("face",user.getFace());
        updateMap.put("fanNickname",user.getNickname());
        updateMap.put("sex",user.getSex());
        updateMap.put("province",user.getProvince());

        IndexRequest ir = new IndexRequest();
        ir.source(updateMap);

        UpdateQuery uq = new UpdateQueryBuilder()
                .withClass(FansEO.class)
                .withId(relationId)
                .withIndexRequest(ir)
                .build();
        esTemplate.update(uq);
    }
}
