package com.hmdp.utils;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/*
* 这个拦截类是为了对其他需要登录的界面进行拦截，防止出现不登陆就访问的情况
* 但是要对登录一系列的开发，使其可以登录（不需要特殊标识就可以进入）
* */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    //就是为了检查特殊标识，在里面进行的内容就是



    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        //获取session
//        HttpSession session = request.getSession();
//        //获取session中的用户
//        Object user =   session.getAttribute("user");
//        //判断用户是否存在
//        if(user == null){
//            response.setStatus(401);
//            return false;
//        }
//        //存在保存信息到threadlocal
//
//        UserHolder.saveUser((UserDTO) user);
//        //放行
        //获取请求头中的token
//        String token = request.getHeader("authorization");
//        if(StrUtil.isBlank(token)){
//            response.setStatus(401);
//            return false;
//        }
//        //获取redis中的用户
//        Map<Object, Object> usermap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
//        //检验用户判断
//        if (usermap.isEmpty()) {
//            response.setStatus(401);
//            return false;
//        }
//
//        //hash转换为dto
//        UserDTO userDTO = BeanUtil.fillBeanWithMap(usermap, new UserDTO(), false);
//        //保存到threadlaocal
//        UserHolder.saveUser(userDTO);
//        //刷新有效期
//        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
//        return true;

        if(UserHolder.getUser() == null){
            response.setStatus(401);
            return false;
        }

        return true;


    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        UserHolder.removeUser();
    }
}
