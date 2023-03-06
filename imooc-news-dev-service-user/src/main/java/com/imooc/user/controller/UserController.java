package com.imooc.user.controller;

import com.imooc.api.BaseController;
import com.imooc.api.controller.user.UserControllerApi;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.pojo.AppUser;
import com.imooc.pojo.bo.UpdateUserInfoBO;
import com.imooc.pojo.vo.AppUserVO;
import com.imooc.pojo.vo.UserAccountInfoVO;
import com.imooc.user.service.UserService;
import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang3.StringUtils;
import org.n3r.idworker.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;


@RestController
@DefaultProperties(defaultFallback = "defaultFallback")
public class UserController extends BaseController implements UserControllerApi {

    final static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    public UserService userService;


    public GraceJSONResult defaultFallback() {
        System.out.println("全局降级：defaultFallback");
        return GraceJSONResult.errorCustom(ResponseStatusEnum.SYSTEM_ERROR_GLOBAL);
    }

    @Override
    public GraceJSONResult getUserInfo(String userId) {
        //0. 判断参数不能为空
        if (StringUtils.isBlank(userId)){
            return GraceJSONResult.errorCustom(ResponseStatusEnum.UN_LOGIN);
        }

        //1. 根据userId查询用户信息
        AppUser user = getUser(userId);

        //2. 返回用户信息
        AppUserVO userVO = new AppUserVO();
        BeanUtils.copyProperties(user,userVO);

        // 3. 查询redis中用户的关注数和粉丝数，放入userVO到前端渲染
        userVO.setMyFansCounts(getCountsFromRedis(REDIS_WRITER_FANS_COUNTS + ":" + userId));
        userVO.setMyFollowCounts(getCountsFromRedis(REDIS_MY_FOLLOW_COUNTS + ":" + userId));


        return GraceJSONResult.ok(userVO);
    }

    @Override
    public GraceJSONResult getAccountInfo(String userId) {

        //0. 判断参数不能为空
        if (StringUtils.isBlank(userId)){
            return GraceJSONResult.errorCustom(ResponseStatusEnum.UN_LOGIN);
        }

        //1. 根据userId查询用户信息
        AppUser user = getUser(userId);

        //2. 返回用户信息
        UserAccountInfoVO accountInfoVO = new UserAccountInfoVO();
        BeanUtils.copyProperties(user,accountInfoVO);


        return GraceJSONResult.ok(accountInfoVO);
    }

    @Override
    public GraceJSONResult updateUserInfo(@Valid UpdateUserInfoBO updateUserInfoBO) {
//        //0. 校验BO
//        if (result.hasErrors()){
//            Map<String,String> map = getErrors(result);
//            return GraceJSONResult.errorMap(map);
//        }
        //1. 执行更新操作
        userService.updateUserInfo(updateUserInfoBO);


        return GraceJSONResult.ok();
    }

    @Value("${server.port}")
    private String myPort;


    @HystrixCommand//(fallbackMethod = "queryByIdsFallback")
    @Override
    public GraceJSONResult queryByIds(String userIds) {

        //1.手动触发异常
//        int a = 1/0;
        //2.模拟超时异常
//        try{
//            Thread.sleep(6000);
//        }catch (Exception e){
//            e.printStackTrace();
//        }

        System.out.println(myPort);

        if (StringUtils.isBlank(userIds)){
            return GraceJSONResult.errorCustom(ResponseStatusEnum.USER_NOT_EXIST_ERROR);
        }

        List<AppUserVO> publisherList = new ArrayList<>();
        List<String> userIdList = JsonUtils.jsonToList(userIds,String.class);
        //FIXME：仅用于dev测试，硬编码动态判断来抛出异常
        if (userIdList.size()>1){
            System.out.println("出现异常~~~~");
            throw new RuntimeException("出现异常~~~");
        }

        for (String userId : userIdList){
            //获得用户基本信息
            AppUserVO userVO = getBasicUserInfo(userId);

            //3.添加到publisherList
            publisherList.add(userVO);
        }


        return GraceJSONResult.ok(publisherList);
    }

    private AppUserVO getBasicUserInfo(String userId){
        //1.根据userId查询用户信息
        AppUser user = getUser(userId);

        //2.返回用户信息
        AppUserVO userVO = new AppUserVO();
        BeanUtils.copyProperties(user,userVO);

        return userVO;
    }

    private AppUser getUser(String userId){
        //查询判断redis中是否包含用户信息，如果包含，则直接查询后返回，就不去查询数据库了
        String userJson = redis.get(REDIS_USER_INFO + ":" + userId);
        AppUser user = null;
        if (StringUtils.isNotBlank(userJson)){
            user = JsonUtils.jsonToPojo(userJson,AppUser.class);
        }else {
            user = userService.getUser(userId);
            //由于用户信息不会经常变动，对于一些千万级别的网站来说，这类信息不会直接去查询数据库
            //那么完全可以依靠redis，直接将查询后的数据放到redis中
            redis.set(REDIS_USER_INFO + ":" + userId, JsonUtils.objectToJson(user));
        }

        return user;
    }



    public GraceJSONResult queryByIdsFallback(String userIds) {

        System.out.println("记录降级方法：queryByIdsFallback");



        List<AppUserVO> publisherList = new ArrayList<>();
        List<String> userIdList = JsonUtils.jsonToList(userIds,String.class);
        for (String userId : userIdList){
            //手动构建空对象，详情页所展示的用户信息可有可无
            AppUserVO userVO = new AppUserVO();
            publisherList.add(userVO);
        }

        return GraceJSONResult.ok(publisherList);
    }



}
