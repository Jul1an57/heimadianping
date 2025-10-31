package com.hmdp.utils;


import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/*
* 这个拦截类是为了对其他需要登录的界面进行拦截，防止出现不登陆就访问的情况
* 但是要对登录一系列的开发，使其可以登录（不需要特殊标识就可以进入）
* */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    //就是为了检查特殊标识，在里面进行的内容就是

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取session
        HttpSession session = request.getSession();
        //获取session中的用户
        Object user =   session.getAttribute("user");
        //判断用户是否存在
        if(user == null){
            response.setStatus(401);
            return false;
        }
        //存在保存信息到threadlocal

        UserHolder.saveUser((UserDTO) user);
        //放行
        return true;


    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        UserHolder.removeUser();
    }
}
