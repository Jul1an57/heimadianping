package com.hmdp.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private ShopTypeMapper shopTypeMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Override
    public Result queryType() {
        //从redis中查询店铺类型
        String key = RedisConstants.CACHE_SHOP_KEY + UUID.randomUUID().toString(true);
        String shopTypeJson = stringRedisTemplate.opsForValue().get(key);
        //查看redis中是否存在
        if (StrUtil.isNotBlank(shopTypeJson)) {
            ShopType shopType = JSONUtil.toBean(shopTypeJson, ShopType.class);
            return Result.ok(shopType);
        }
        //不存在在数据库里查询
        List<ShopType> shopTypelist = query().orderByAsc("sort").list();
        //查看是否存在
        if (shopTypelist == null || shopTypelist.size() == 0) {
            return Result.fail("查询失败，没有该商铺类型");
        }
        //存入redis中
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopTypelist));
        return Result.ok(shopTypelist);
    }
}
