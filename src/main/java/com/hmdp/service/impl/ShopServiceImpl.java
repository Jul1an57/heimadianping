package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.injector.methods.UpdateById;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

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
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
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
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //在数据库在，写入redis，不在返回404
        //在，返回
        return Result.ok(shop);
    }

    @Override
    @Transactional()
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺ID不能为空");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }
}
