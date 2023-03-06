package com.imooc.admin.service;

import com.imooc.pojo.AdminUser;
import com.imooc.pojo.bo.NewAdminBO;
import com.imooc.pojo.mo.FriendLinkMO;
import com.imooc.utils.PagedGridResult;

import java.util.List;

public interface FriendLinkService {

    /**
     * 新增或者更新友情链接
     */
    public void saveOrUpdateFriendLink(FriendLinkMO friendLinkMO);

    /**
     * 查询友情链接
     */
    public List<FriendLinkMO> queryAllFriendLinkList();

    /**
     * 删除友情链接
     */
    public void delete(String linkId);

    /**
     * 首页查询友情链接
     */
    public List<FriendLinkMO> queryPortalAllFriendLinkList();
}
