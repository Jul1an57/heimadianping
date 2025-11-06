package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryById(Long id) {

        //从redis中查询缓存，
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY);
        //判断缓存是否在存在
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        Shop shop = getById(id);
        //不在，根据id在数据库查询，
        if(shop == null){
            return Result.fail("店铺不存在");
        }
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY, JSONUtil.toJsonStr(shop));
        //在数据库在，写入redis，不在返回404
        //在，返回
        return Result.ok(shop);
    }
}
