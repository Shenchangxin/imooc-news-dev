package com.imooc.user.service;

import com.imooc.enums.Sex;
import com.imooc.pojo.AppUser;
import com.imooc.pojo.bo.UpdateUserInfoBO;
import com.imooc.pojo.vo.FansCountsVO;
import com.imooc.pojo.vo.RegionRatioVO;
import com.imooc.utils.PagedGridResult;

import java.util.List;


public interface MyFanService {

    /**
     * 查询当前用户是否为作家粉丝
     */
    public boolean isMeFollowThisWriter(String writerId,String fanId);

    /**
     * 关注成为粉丝
     */
    public void follow(String writerId,String fanId);

    /**
     * 取消关注
     */
    public void unfollow(String writerId,String fanId);

    /**
     * 查询我的粉丝数
     */
    public PagedGridResult queryMyFansList(String writerId, Integer page, Integer pageSize);

    public PagedGridResult queryMyFansESList(String writerId, Integer page, Integer pageSize);

    /**
     * 查询粉丝数
     */
    public Integer queryFansCounts(String writerId, Sex sex);

    public FansCountsVO queryFansESCounts(String writerId);

    /**
     * 根据地区查询粉丝数
     */
    public List<RegionRatioVO> queryRegionRatioCounts(String writerId);
    public List<RegionRatioVO> queryRegionRatioESCounts(String writerId);

    /**
     * 被动更新
     */
    public void forceUpdateFanInfo(String relationId,String fanId);

}
