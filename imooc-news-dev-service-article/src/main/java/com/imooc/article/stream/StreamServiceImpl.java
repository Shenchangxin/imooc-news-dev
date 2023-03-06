package com.imooc.article.stream;

import com.imooc.pojo.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@EnableBinding(MyStreamChannel.class)
public class StreamServiceImpl implements StreamService{

    @Autowired
    private MyStreamChannel myStreamChannel;
    @Override
    public void sendMsg() {
        AppUser user = new AppUser();
        user.setId("10101");
        user.setNickname("imooc");

        //消息通过绑定器发给mq
        myStreamChannel.output().send(MessageBuilder.withPayload(user).build());

    }

    @Override
    public void eat(String dumpling) {
        myStreamChannel.output().send(MessageBuilder.withPayload(dumpling).build());
    }

}
