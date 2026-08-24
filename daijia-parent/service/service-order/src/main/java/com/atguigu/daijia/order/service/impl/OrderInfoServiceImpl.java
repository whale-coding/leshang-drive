package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.entity.order.OrderStatusLog;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Resource
    private OrderInfoMapper orderInfoMapper;

    @Resource
    private OrderStatusLogMapper orderStatusLogMapper;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 保存订单信息(乘客下单)
     * @param orderInfoForm 订单信息
     * @return 订单id
     */
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        OrderInfo orderInfo = new OrderInfo();

        BeanUtils.copyProperties(orderInfoForm, orderInfo);
        String orderNo = UUID.randomUUID().toString().replaceAll("-","");  // 订单号
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        orderInfo.setOrderNo(orderNo);
        // 插入数据库
        orderInfoMapper.insert(orderInfo);

        // 记录日志
        this.log(orderInfo.getId(), orderInfo.getStatus());

        // 向Redis中添加标识
        // 接单标识，标识不存在了说明不在等待接单状态了
        redisTemplate.opsForValue().set(RedisConstant.ORDER_ACCEPT_MARK, "0", RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);

        // 返回订单id
        return orderInfo.getId();
    }

    /**
     * 查询订单状态
     * @param orderId 订单id
     * @return 订单状态（0/1/2）
     */
    @Override
    public Integer getOrderStatus(Long orderId) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.select(OrderInfo::getStatus);

        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);

        if(null == orderInfo) {
            // 返回null，feign解析会抛出异常，给默认值，后续会用
            return OrderStatus.NULL_ORDER.getStatus();
        }
        return orderInfo.getStatus();
    }

    /**
     * 记录日志
     * @param orderId 订单id
     * @param status 状态
     */
    private void log(Long orderId, Integer status) {
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperateTime(new Date());
        // 插入数据库
        orderStatusLogMapper.insert(orderStatusLog);
    }

    /**
     * 司机抢单
     * @param driverId 司机ID
     * @param orderId 订单ID
     * @return 是否抢单成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean robNewOrder(Long driverId, Long orderId) {
        // 判断订单是否存在，通过Redis判断（看保存订单时），减少数据库的压力
        // 抢单成功或取消订单，都会删除该key，redis判断，减少数据库压力
        if(!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
            // 抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }
        // 司机抢单
        // 修改order_info表订单状态、接单司机id、接单实践
        // update order_info set status = 2, driver_id = #{driverId}, accept_time = now() where id = #{id}
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());  // 修改订单状态值为2，表示“已接单”
        orderInfo.setAcceptTime(new Date());
        orderInfo.setDriverId(driverId);

        int rows = orderInfoMapper.updateById(orderInfo);
        if(rows != 1) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        // 记录日志
        this.log(orderId, orderInfo.getStatus());

        // 删除redis订单标识
        redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);

        return true;
    }
}
