package com.imooc.zuul.filters;

import com.imooc.grace.result.GraceJSONResult;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.utils.IPUtil;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.RedisOperator;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 构建zuul的自定义过滤器
 */
@Component
@RefreshScope
public class BlackIpFilter extends ZuulFilter {


    @Value("${blackIp.continueCounts}")
    private Integer continueCounts;
    @Value("${blackIp.timeInterval}")
    private Integer timeInterval;
    @Value("${blackIp.limitTimes}")
    private Integer limitTimes;
    @Autowired
    private RedisOperator redis;
    @Override
    public String filterType() {
        return "pre";
    }


    @Override
    public int filterOrder() {
        return 2;
    }


    @Override
    public boolean shouldFilter() {
        return true;
    }


    @Override
    public Object run() throws ZuulException {

        //获得上下文对象
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();

        //获得IP
        String ip = IPUtil.getRequestIp(request);

        /**
         * 需求：
         *      判断IP在10秒内的请求次数是否超过10次
         *      如果超过，则限制这个IP访问15秒，15秒以后在放行
         */
        final String ipRedisKey = "zuul-ip:" + ip;
        final String ipRedisLimitKey = "zuul-ip-limit:" + ip;

        //获得当前IP的这个key的剩余时间
        long limitLeftTime = redis.ttl(ipRedisLimitKey);
        //如果当前限制IP的key还存在剩余时间，说明这个IP不能访问，继续等待
        if (limitLeftTime > 0){
            stopRequest(context);
            return null;
        }
        //在redis中累加ip的请求次数
        long requestCounts = redis.increment(ipRedisKey,1);
        //从0开始计算请求次数，初期访问为1，则设置过期时间，也就是连续请求的间隔时间
        if (requestCounts == 1){
            redis.expire(ipRedisKey,timeInterval);
        }

        //如果还能取得请求次数，说明用户连续请求的次数落在10s内
        //一旦请求次数超过了连续访问的次数，则需要限制这个IP的访问
        if (requestCounts > continueCounts){
            //限制IP的访问时间
            redis.set(ipRedisLimitKey,ipRedisLimitKey,limitTimes);
            stopRequest(context);
        }

        return null;//没有任何意义，不用管
    }

    private void stopRequest(RequestContext context){
        //停止zuul继续向下路由，禁止请求通信
        context.setSendZuulResponse(false);
        context.setResponseStatusCode(200);
        String result = JsonUtils.objectToJson(GraceJSONResult.errorCustom(ResponseStatusEnum.SYSTEM_ERROR_ZUUL));
        context.setResponseBody(result);
        context.getResponse().setCharacterEncoding("utf-8");
        context.getResponse().setContentType(MediaType.APPLICATION_JSON_VALUE);
    }
}
