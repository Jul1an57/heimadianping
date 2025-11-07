package com.hmdp.utils;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;
@Component
public class ReflashtokenInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public ReflashtokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取请求头中的token
        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)){
           return true;
        }
        //获取redis中的用户
        Map<Object, Object> usermap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
        //检验用户判断
        if (usermap.isEmpty()) {
          return true;
        }

        //hash转换为dto
        UserDTO userDTO = BeanUtil.fillBeanWithMap(usermap, new UserDTO(), false);
        //保存到threadlaocal
        UserHolder.saveUser(userDTO);
        //刷新有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }
}
