package com.imooc.interceptors;

import com.imooc.exception.GraceException;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.utils.IPUtil;
import com.imooc.utils.RedisOperator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class ArticleReadInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisOperator redis;

    public static final String REDIS_ALREADY_READ = "redis_already_read";
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String articleId = request.getParameter("articleId");
        String userIp = IPUtil.getRequestIp(request);
        //设置针对当前用户ip的永久存在的key，存入到redis，表示该ip用户已经阅读过了，无法增加阅读量
        boolean isExist = redis.keyIsExist(REDIS_ALREADY_READ + ":" + articleId + ":" + userIp);
        if (isExist){
            return false;
        }


        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
