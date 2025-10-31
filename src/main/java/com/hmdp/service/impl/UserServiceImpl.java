package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;
import static org.springframework.beans.BeanUtils.copyProperties;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //检验手机号
        //不合格返回失败
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式有误");
        }
        //合格的话生成随机验证码
        String code = RandomUtil.randomNumbers(6);
        //把验证码保存到session（为了方便之后前端输入验证码和后端的session的验证码进行比对）
        session.setAttribute("code", code);
        //发送验证码
        log.debug("发送验证码成功,验证码：{}",code);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //验证验证码
        String logincode = loginForm.getCode();
        String code = (String) session.getAttribute("code");
        if(!logincode.equals(code) || logincode == null) {
            return Result.fail("验证码输入有误");
        }
        String phone = loginForm.getPhone();
        User user = query().eq("phone", phone).one();

        if(user == null) {
            user = new User();
            user.setPhone(phone);
            user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
            save(user);
        }

        session.setAttribute("user", BeanUtil.copyProperties(user,UserDTO.class));
        return Result.ok();
    }
}
