package com.imooc.api.config;

import com.imooc.interceptors.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Bean
    public PassportInterceptor passportInterceptor(){
        return new PassportInterceptor();
    }

    @Bean
    public ArticleReadInterceptor articleReadInterceptor(){
        return new ArticleReadInterceptor();
    }


    @Bean
    public UserTokenInterceptor userTokenInterceptor(){
        return new UserTokenInterceptor();
    }

    @Bean
    public AdminTokenInterceptor adminTokenInterceptor(){
        return new AdminTokenInterceptor();
    }

    @Bean
    public UserActiveInterceptor userActiveInterceptor(){
        return new UserActiveInterceptor();
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(passportInterceptor())
                .addPathPatterns("/passport/getSMSCode");

        registry.addInterceptor(userTokenInterceptor())
                .addPathPatterns("/user/getAccountInfo")
                .addPathPatterns("/fs/uploadFace")
                .addPathPatterns("/user/updateUserInfo")
                .addPathPatterns("/fs/uploadSomeFiles")
                .addPathPatterns("/fans/follow")
                .addPathPatterns("/fans/unfollow");

        registry.addInterceptor(userActiveInterceptor())
                .addPathPatterns("/fs/uploadSomeFiles")
                .addPathPatterns("/fans/follow")
                .addPathPatterns("/fans/unfollow");

        registry.addInterceptor(articleReadInterceptor())
                .addPathPatterns("/portal/article/readArticle");

        registry.addInterceptor(adminTokenInterceptor())
                .addPathPatterns("/adminMng/adminIsExist")
                .addPathPatterns("/adminMng/getAdminList")
                .addPathPatterns("/adminMng/addNewAdmin")
                .addPathPatterns("/fs/uploadToGridFS")
                .addPathPatterns("/fs/readInGridFS")
                .addPathPatterns("/friendLinkMng/saveOrUpdateFriendLink")
                .addPathPatterns("/friendLinkMng/getFriendLinkList")
                .addPathPatterns("/friendLinkMng/delete")
                .addPathPatterns("/categoryMng/saveOrUpdateCategory")
                .addPathPatterns("/categoryMng/getCatList");
                //.addPathPatterns("/friendLinkMng/portal/list");
    }
}
