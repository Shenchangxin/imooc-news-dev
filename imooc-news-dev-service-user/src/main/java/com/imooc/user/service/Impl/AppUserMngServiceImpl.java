package com.imooc.user.service.Impl;

import com.github.pagehelper.PageHelper;
import com.imooc.api.service.BaseService;
import com.imooc.enums.Sex;
import com.imooc.enums.UserStatus;
import com.imooc.exception.GraceException;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.pojo.AppUser;
import com.imooc.pojo.bo.UpdateUserInfoBO;
import com.imooc.user.mapper.AppUserMapper;
import com.imooc.user.service.AppUserMngService;
import com.imooc.user.service.UserService;
import com.imooc.utils.DateUtil;
import com.imooc.utils.DesensitizationUtil;
import com.imooc.utils.PagedGridResult;
import com.imooc.utils.RedisOperator;
import org.apache.commons.lang3.StringUtils;
import org.n3r.idworker.Sid;
import org.n3r.idworker.utils.JsonUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

@Service
public class AppUserMngServiceImpl extends BaseService implements AppUserMngService {

    @Autowired
    public AppUserMapper appUserMapper;

    @Override
    public PagedGridResult queryAllUserList(String nickname, Integer status, Date startDate, Date endDate, Integer page, Integer pageSize) {

         Example example = new Example(AppUser.class);
         example.orderBy("createdTime").desc();
         Example.Criteria criteria = example.createCriteria();

         if (StringUtils.isNotBlank(nickname)){
             criteria.andLike("nickname","%"+ nickname +"%");
         }
         if (UserStatus.isUserStatusValid(status)){
             criteria.andEqualTo("activeStatus",status);
         }

         if (startDate != null){
             criteria.andGreaterThanOrEqualTo("createdTime",startDate);
         }
        if (endDate != null){
            criteria.andLessThanOrEqualTo("createdTime",endDate);
        }

        PageHelper.startPage(page,pageSize);
        List<AppUser> list = appUserMapper.selectByExample(example);


        return setterPagedGrid(list,page);
    }

    @Transactional
    @Override
    public void freezeUserOrNot(String userId, Integer doStatus) {
        AppUser user = new AppUser();
        user.setId(userId);
        user.setActiveStatus(doStatus);
        appUserMapper.updateByPrimaryKeySelective(user);
    }


}
