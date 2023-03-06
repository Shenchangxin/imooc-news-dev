package com.imooc.user.service;

import com.imooc.pojo.AppUser;
import com.imooc.pojo.bo.UpdateUserInfoBO;
import com.imooc.utils.PagedGridResult;

import java.util.Date;


public interface AppUserMngService {


    /**
     * 查询管理员列表
     */
    public PagedGridResult queryAllUserList(String nickname,
                                            Integer status,
                                            Date startDate,
                                            Date endDate,
                                            Integer page,
                                            Integer pageSize);

    /**
     * 冻结用户账号或者解除冻结
     */
    public void freezeUserOrNot(String userId,Integer doStatus);

}
