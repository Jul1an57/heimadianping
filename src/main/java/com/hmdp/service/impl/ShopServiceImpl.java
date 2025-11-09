package com.hmdp.service.impl;

import ch.qos.logback.core.util.TimeUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.injector.methods.UpdateById;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
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
        //缓存穿透

        //互斥锁解决缓存击穿
        Shop shop = queryWithMutext(id);

            return Result.ok(shop);
    }

    //redis穿透的店铺查询
    public Shop queryWithMutext(Long id)  {
        //从redis中查询缓存，
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //判断缓存是否在存在
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //命中的是否为空置
        if(shopJson != null){
            return null;
        }
        //实现缓存重建
        //获取互斥锁
        Boolean tryLock = TryLock(RedisConstants.LOCK_SHOP_KEY + id);
        Shop shop;
        try {
            //判断获取是否成功
            if(!tryLock){
                //失败，休眠并重试
                Thread.sleep(50);
                return queryWithMutext(id);
            }

            //成功，根据id查询数据库
            shop = getById(id);
            //不在，根据id在数据库查询，
            if(shop == null){
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"", RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
            //在数据库在，写入redis，不在返回404
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            UnLock(RedisConstants.LOCK_SHOP_KEY + id);
        }
        //在，返回
        return shop;
    }


    //redis穿透的店铺查询
    public Shop queryWithPassThrough(Long id){
        //从redis中查询缓存，
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //判断缓存是否在存在
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //命中的是否为空置
        if(shopJson != null){
            return null;

        }
        Shop shop = getById(id);
        //不在，根据id在数据库查询，
        if(shop == null){
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"", RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //在数据库在，写入redis，不在返回404
        //在，返回
        return shop;
    }


    //互斥锁的获取
    private Boolean TryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    //互斥锁的释放
    private void UnLock(String key){
        stringRedisTemplate.delete(key);
    }
    @Override
    @Transactional
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
