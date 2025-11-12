package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIDWorker {

    private final StringRedisTemplate stringRedisTemplate;


    public RedisIDWorker(StringRedisTemplate redisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    //开始的时间戳
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    //序列号位数
    private static final long COUNT_BITS = 32;
        public Long nextId(String keyPrefix){
            //生成时间戳（当前时间-基础时间）
            LocalDateTime now = LocalDateTime.now();
            long nowsecond = now.toEpochSecond(ZoneOffset.UTC);//将多少年月日转换为秒，UTC为o失去偏移量
            long timestamp = nowsecond - BEGIN_TIMESTAMP;
            //生成序列号
            //获取当天的年月日,获取这个date是为了防止自增长key不变随着时间增长出现问题，因为一开始的时间戳是没有存在redis中的

            String date = now.format(DateTimeFormatter.ofPattern("yyyy-MM:dd"));
            long increment = stringRedisTemplate.opsForValue().increment("icr" + keyPrefix + ":" + date);//incre是自增的意思

            //拼接并返回
            // << 是向左移位的意思，为了给序列号留下32个比特的空位，所以这么操作，然后采用 | 运算是因为 00000000 进行或运算是什么就会输出什么
            return timestamp << COUNT_BITS | increment;
        }

        public static void main(String[] args) {
            LocalDateTime time = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
            long second = time.toEpochSecond(ZoneOffset.UTC);
            System.out.println(second);
        }
}
