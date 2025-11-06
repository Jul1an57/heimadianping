package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
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
    @Resource
    private StringRedisTemplate stringRedisTemplate;
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

        //保存到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码
        log.debug("发送验证码成功,验证码：{}",code);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //验证验证码
        String logincode = loginForm.getCode();
        String phone = loginForm.getPhone();
        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if(!logincode.equals(code) || logincode == null) {
            return Result.fail("验证码输入有误");
        }

        User user = query().eq("phone", phone).one();

        if(user == null) {
            user = new User();
            user.setPhone(phone);
            user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
            save(user);
        }

        //session.setAttribute("user", BeanUtil.copyProperties(user,UserDTO.class));
        //保存用到redis
        //生成随机的token作为登录令牌
        String token = UUID.randomUUID().toString(true);

        //将user对象转换为hash存储(隐藏用户信息)
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> usermap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token,usermap);
        //设置有效期
        //如果只设置token的保持时间，那么如果到了30分钟就会自动退
        //拦截器会一直接收访问任何一个，所以每次接受的时候都刷新。
        stringRedisTemplate.expire(LOGIN_USER_KEY + token,LOGIN_USER_TTL, TimeUnit.SECONDS);
        return Result.ok(token);
    }
}
