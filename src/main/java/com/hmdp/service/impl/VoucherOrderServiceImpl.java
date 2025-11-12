package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIDWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

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

    @Override
    @Transactional
    public Result scvillVoucher(Long voucherId) {

        //查询用户券信息
        //采取这个是因为
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //判断秒杀是否开始
        LocalDateTime beginTime = voucher.getBeginTime();
        LocalDateTime endTime = voucher.getEndTime();
        LocalDateTime now = LocalDateTime.now();
        //没开始或者已结束返回异常
        if(now.isBefore(beginTime) || now.isAfter(endTime)) {

            return Result.fail("秒杀活动目前神秘");
        }
        //活动时间内判断是否还有库存，且是否满足购买的数量
        int stock = voucher.getStock();
        if(stock <= 0) {
            return Result.fail("库存不足，无法购买");
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
        Long userid = UserHolder.getUser().getId();
        voucherOrder.setUserId(userid);
        //订单id,如果不采用这种方式，那么用户查询对应的id的时候就会看到总的信息，所以用了
        Long orderid = redisIDWorker.nextId("order");
        voucherOrder.setId(orderid);

        save(voucherOrder);
        return Result.ok(voucherOrder);
    }
}
