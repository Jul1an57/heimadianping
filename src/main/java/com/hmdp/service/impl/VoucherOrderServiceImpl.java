package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIDWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.Synchronized;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIDWorker redisIDWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Override
    public Result scvillVoucher(Long voucherId) throws InterruptedException {

        //查询用户券信息
        //采取这个是因为
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //判断秒杀是否开始
        LocalDateTime beginTime = voucher.getBeginTime();
        LocalDateTime endTime = voucher.getEndTime();
        LocalDateTime now = LocalDateTime.now();

        //没开始或者已结束返回异常
        if (now.isBefore(beginTime) || now.isAfter(endTime)) {

            return Result.fail("秒杀活动目前神秘");
        }
        //活动时间内判断是否还有库存，且是否满足购买的数量
        int stock = voucher.getStock();
        if (stock <= 0) {
            return Result.fail("库存不足，无法购买");
        }

        Long userid = UserHolder.getUser().getId();
        //采取这个方法是因为锁要对象相同而不是值相同，都是同一个id其对应的值是相等的但是多线程对应的地址是不同的会锁住
        //转换为字符串的形式，每创建一个都是一个新对象，所以要用。intern（），确保相同内容的字符串最终指向同一个对象
        //把他从上面提出来是因为这个锁要是防止函数标题上那就说明整个函数都被锁了
        //要是放在里面，因为有事务，当锁已经释放了，但是事务还没有提交的时候，依然会有并发问题
        //就是在悲观锁释放后因为事务还没提交此时再有线程进来的话，就会出现并发问题，超卖，不是单买


        //synchronized(userid.toString().intern()) {}
        /*
         *这个return返回的是this.createVoucherOrder(voucherId);这样是有问题的，这里this相当于调用的是原对象
         * 因为注解的事务是由aop进行代理的，所以要采用代理对象而不是原对象.(代理对象就是对原对象的一个封装，添加了一些功能)
         *解决方法：
         * 1.自己注入自己，注入自己后，调用一个方法获取bean代理对象
         * 2.用aopcontext获取自己的代理对象
         * */
        //return this.createVoucherOrder(voucherId);

        //用分布式锁的方式来解决(自己手写版)
//        SimpleRedisLock lock = new SimpleRedisLock("order" + userid, stringRedisTemplate);
//        boolean islock = lock.tryLock(1200);
//        if(!islock) {
//            return Result.fail("不允许重复下单");
//        }
//
//        try {
//            //Spring默认使用面向接口编程的原则，代理对象基于接口生成，而不是具体实现类。所以这里采用的是service来接收代理对象
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
//            lock.unlock();
//        }
        /*
         * 采用redisson实现分布式锁
         * */
        RLock lock = redissonClient.getLock("lock:order" + userid);

        boolean islock = lock.tryLock(1, 10, TimeUnit.SECONDS);
        if (!islock) {
            return Result.fail("不允许重复下单");
        }
        try {
            IVoucherOrderService  proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }finally {
            lock.unlock();
        }
    }

//创建一个优惠券的订单,采用悲观锁是因为这是防止一个用户多买，用乐观锁的情况是防止超卖问题（多个用户）
    @Transactional
    public  Result createVoucherOrder(Long voucherId) {
        //判断是不是第一次购买
        Long userid = UserHolder.getUser().getId();

        int count = query().eq("user_id", userid).eq("voucher_id", voucherId).count();
        if(count > 0) {
            return Result.fail("该用户已经购买过");
        }
        //扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1 ")
                .eq("voucher_id", voucherId)
                .update();
        //再次判断
        if(!success) {
            return Result.fail("库存不足，无法购买");
        }


        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setVoucherId(voucherId);
        //在userhold中获取线程中的对应id。

        voucherOrder.setUserId(userid);
        //订单id,如果不采用这种方式，那么用户查询对应的id的时候就会看到总的信息，所以用了
        Long orderid = redisIDWorker.nextId("order");
        voucherOrder.setId(orderid);

        save(voucherOrder);
        return Result.ok(voucherOrder);

    }


}
