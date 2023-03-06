package com.imooc.article.service.impl;

import com.github.pagehelper.PageHelper;
import com.imooc.api.service.BaseService;
import com.imooc.article.mapper.ArticleMapper;
import com.imooc.article.mapper.CommentsMapper;
import com.imooc.article.mapper.CommentsMapperCustom;
import com.imooc.article.service.ArticlePortalService;
import com.imooc.article.service.CommentPortalService;
import com.imooc.enums.ArticleReviewStatus;
import com.imooc.enums.YesOrNo;
import com.imooc.pojo.Article;
import com.imooc.pojo.Comments;
import com.imooc.pojo.vo.ArticleDetailVO;
import com.imooc.pojo.vo.CommentsVO;
import com.imooc.utils.PagedGridResult;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.C;
import org.n3r.idworker.Sid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.xml.stream.events.Comment;
import java.util.*;

@Service
public class CommentPortalServiceImpl extends BaseService implements CommentPortalService {


    @Autowired
    private CommentsMapper commentsMapper;

    @Autowired
    private CommentsMapperCustom commentsMapperCustom;

    @Autowired
    private Sid sid;

    @Autowired
    private ArticlePortalService articlePortalService;

    @Transactional
    @Override
    public void createComment(String articleId, String fatherCommentId, String content, String userId, String nickname,String face) {

        String commentId = sid.nextShort();

        ArticleDetailVO article
                = articlePortalService.queryDetail(articleId);

        Comments comments = new Comments();
        comments.setId(commentId);

        comments.setWriterId(article.getPublishUserId());
        comments.setArticleTitle(article.getTitle());
        comments.setArticleCover(article.getCover());
        comments.setArticleId(articleId);

        comments.setFatherId(fatherCommentId);
        comments.setCommentUserId(userId);
        comments.setCommentUserNickname(nickname);
        comments.setCommentUserFace(face);

        comments.setContent(content);
        comments.setCreateTime(new Date());

        commentsMapper.insert(comments);

        //评论数量累加
        redis.increment(REDIS_ARTICLE_COMMENT_COUNTS + ":" + articleId,1);
    }

    @Override
    public PagedGridResult queryArticleComments(String articleId, Integer page, Integer pageSize) {

        Map<String, Object> map = new HashMap<>();
        map.put("articleId",articleId);
        PageHelper.startPage(page,pageSize);
        List<CommentsVO> list = commentsMapperCustom.queryArticleCommentList(map);

        return setterPagedGrid(list,page);
    }

    @Override
    public PagedGridResult queryWriterCommentsMng(String writerId, Integer page, Integer pageSize) {

        Comments comment = new Comments();
        comment.setWriterId(writerId);

        PageHelper.startPage(page, pageSize);
        List<Comments> list = commentsMapper.select(comment);
        return setterPagedGrid(list, page);
    }

    @Override
    public void deleteComment(String writerId, String commentId) {
        Comments comments = new Comments();
        comments.setId(commentId);
        comments.setWriterId(writerId);

        commentsMapper.delete(comments);
    }
}
