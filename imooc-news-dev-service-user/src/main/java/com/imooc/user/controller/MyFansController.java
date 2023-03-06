package com.imooc.user.controller;

import com.imooc.api.BaseController;
import com.imooc.api.controller.user.HelloControllerApi;
import com.imooc.api.controller.user.MyFansControllerApi;
import com.imooc.enums.Sex;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.pojo.vo.FansCountsVO;
import com.imooc.user.service.MyFanService;
import com.imooc.utils.RedisOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MyFansController extends BaseController implements MyFansControllerApi {

    final static Logger logger = LoggerFactory.getLogger(MyFansController.class);


    @Autowired
    private MyFanService myFanService;

    @Override
    public GraceJSONResult isMeFollowThisWriter(String writer, String fanId) {

        boolean res = myFanService.isMeFollowThisWriter(writer,fanId);

        return GraceJSONResult.ok(res);
    }

    @Override
    public GraceJSONResult follow(String writerId, String fanId) {

        myFanService.follow(writerId,fanId);
        return GraceJSONResult.ok();
    }

    @Override
    public GraceJSONResult unfollow(String writerId, String fanId) {

        myFanService.unfollow(writerId,fanId);
        return GraceJSONResult.ok();

    }

    @Override
    public GraceJSONResult queryAll(String writerId, Integer page, Integer pageSize) {

        if (page == null){
            page=COMMON_START_PAGE;
        }
        if (pageSize == null) {
            page = COMMON_PAGE_SIZE;
        }


        return GraceJSONResult.ok(myFanService.queryMyFansList(writerId,page,pageSize));
    }

    @Override
    public GraceJSONResult queryRatio(String writerId) {
        int manCounts = myFanService.queryFansCounts(writerId, Sex.man);
        int womanCounts = myFanService.queryFansCounts(writerId, Sex.woman);

        FansCountsVO fansCountsVO = new FansCountsVO();
        fansCountsVO.setManCounts(manCounts);
        fansCountsVO.setWomanCounts(womanCounts);


        return GraceJSONResult.ok(fansCountsVO);
    }

    @Override
    public GraceJSONResult queryRatioByRegion(String writerId) {


        return GraceJSONResult.ok(myFanService.queryRegionRatioCounts(writerId));
    }
}
