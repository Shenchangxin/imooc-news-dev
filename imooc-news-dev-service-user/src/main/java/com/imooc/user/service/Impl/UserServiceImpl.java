package com.imooc.user.service.Impl;

import com.imooc.enums.Sex;
import com.imooc.enums.UserStatus;
import com.imooc.exception.GraceException;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.pojo.AppUser;
import com.imooc.pojo.bo.UpdateUserInfoBO;
import com.imooc.user.mapper.AppUserMapper;
import com.imooc.user.service.UserService;
import com.imooc.utils.DateUtil;
import com.imooc.utils.DesensitizationUtil;
import com.imooc.utils.RedisOperator;
import org.n3r.idworker.Sid;
import org.n3r.idworker.utils.JsonUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private AppUserMapper appUserMapper;

    @Autowired
    public RedisOperator redis;

    public  static final String REDIS_USER_INFO = "redis_user_info";

    @Autowired
    public Sid sid;

    private static final String USER_FACE0 = "http://122.152.205.72:88/group1/M00/00/05/CpoxxFw_8_qAIlFXAAAcIhVPdSg994.png";
    private static final String USER_FACE1 = "http://122.152.205.72:88/group1/M00/00/05/CpoxxF6ZUySASMbOAABBAXhjY0Y649.png";
    private static final String USER_FACE2 = "http://122.152.205.72:88/group1/M00/00/05/CpoxxF6ZUx6ANoEMAABTntpyjOo395.png";
    @Override
    public AppUser queryMobileIsExist(String mobile) {
        Example userExample = new Example(AppUser.class);
        Example.Criteria userCriteria = userExample.createCriteria();
        userCriteria.andEqualTo("mobile",mobile);
        AppUser user = appUserMapper.selectOneByExample(userExample);
        return user;
    }

    @Transactional
    @Override
    public AppUser createUser(String mobile) {

        /**
         * 互联网项目都要考虑可扩展性
         * 如果未来的业务激增，那么就需要分库分表
         * 那么数据库表主键id必须保证全局（全库）唯一，不得重复
         */
        String userId = sid.nextShort();

        AppUser appUser = new AppUser();

        appUser.setId(userId);
        appUser.setMobile(mobile);
        appUser.setNickname("用户："+ DesensitizationUtil.commonDisplay(mobile));
        appUser.setFace(USER_FACE0);
        appUser.setBirthday(DateUtil.stringToDate("1900-01-01"));
        appUser.setSex(Sex.secret.type);
        appUser.setActiveStatus(UserStatus.INACTIVE.type);
        appUser.setTotalIncome(0);
        appUser.setCreatedTime(new Date());
        appUser.setUpdatedTime(new Date());
        appUserMapper.insert(appUser);

        return appUser;
    }

    @Override
    public AppUser getUser(String userId) {


        return appUserMapper.selectByPrimaryKey(userId);
    }

    @Override
    public void updateUserInfo(UpdateUserInfoBO updateUserInfoBO) {
        String userId = updateUserInfoBO.getId();
        //保证双写一致，先删除redis中的数据，后更新数据库
        redis.del(REDIS_USER_INFO + ":" + userId);

        AppUser userInfo = new AppUser();
        BeanUtils.copyProperties(updateUserInfoBO,userInfo);

        userInfo.setUpdatedTime(new Date());
        userInfo.setActiveStatus(UserStatus.ACTIVE.type);

        int result = appUserMapper.updateByPrimaryKeySelective(userInfo);
        if (result != 1){
            GraceException.display(ResponseStatusEnum.USER_UPDATE_ERROR);
        }

        //再次查询用户最新信息，放入redis中
        AppUser user = getUser(userId);
        redis.set(REDIS_USER_INFO + ":" + userId, JsonUtils.objectToJson(user));

        //缓存双删策略
        try{
            Thread.sleep(100);
            redis.del(REDIS_USER_INFO + ":" + userId);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
