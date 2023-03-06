package com.imooc.article.service;


import com.imooc.pojo.Article;
import com.imooc.pojo.vo.ArticleDetailVO;
import com.imooc.utils.PagedGridResult;

import java.util.List;

public interface CommentPortalService {

    /**
     * 发表评论
     */
    public void createComment(String articleId, String fatherCommentId, String content, String userId,String nickname,String face);

    /**
     * 查询评论列表
     */
    public PagedGridResult queryArticleComments(String articleId, Integer page, Integer pageSize);

    /**
     * 查询我的评论管理列表
     */
    public PagedGridResult queryWriterCommentsMng(String writerId, Integer page, Integer pageSize);

    /**
     * 删除评论
     */
    public void deleteComment(String writerId, String commentId);


}
