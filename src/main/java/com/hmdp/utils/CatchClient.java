package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;


import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

@Component
@Slf4j
public class CatchClient {

    //因为是final构造，不可更改，所以采用构造器构造，而不是自动注入
    private final StringRedisTemplate stringRedisTemplate;

    public CatchClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
//往redis中注入ttl数据
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }
//逻辑过期数据
    public void setLogical(String key, Object value, Long time, TimeUnit unit) {

        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        redisData.setData(value);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R,ID>R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,
            Long time, TimeUnit unit
    ){
        String key = keyPrefix+id;
        //从redis中查询缓存，
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断缓存是否在存在
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        //命中的是否为空置
        if(json != null){
            return null;
        }
        R r = dbFallback.apply(id);
        //不在，根据id在数据库查询，
        if(r == null){
            stringRedisTemplate.opsForValue().set(key,"", RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        this.set(key, r, time, unit);

        return r;
    }



    //线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    //逻辑过期解决缓存击穿问题
    public <R,ID> R queryWithLogicalExpire(String keyPrefix,ID id,Class<R> type,Function<ID,R> dbFallback,
                                           String lockPrefix,Long time, TimeUnit unit){
        //从redis中查询缓存，
        String json = stringRedisTemplate.opsForValue().get(keyPrefix + id);
        //判断缓存是否在存在
        if (StrUtil.isBlank(json)) {
            return null;
        }
        //命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        R r = JSONUtil.toBean(data, type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //未过期直接返回
            return r;
        }
        //已过期
        //缓存重建
        //获取互斥锁
        String lockKey = lockPrefix + id;
        Boolean islock = TryLock(lockKey);
        //是否成功
        if(islock){
            //成，开启独立线程
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //查询数据库
                    R r1 = dbFallback.apply(id);
                    //写入缓存
                    this.setLogical(keyPrefix + id,r1,time,unit);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
                finally {
                    UnLock(lockKey);
                }
            });
        }
        //返回商户信息
        return r;
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

}
