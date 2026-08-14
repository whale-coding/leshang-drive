package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;

    @Resource
    private NewOrderFeignClient newOrderFeignClient;

    /**
     * 查询订单状态
     * @param orderId 订单id
     * @return 订单状态
     */
    @Override
    public Integer getOrderStatus(Long orderId) {
        // 远程调用
        return orderInfoFeignClient.getOrderStatus(orderId).getData();
    }

    /**
     * 查询司机新订单数据
     * @param driverId 司机ID
     * @return 新订单数据
     */
    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        // 远程调用
        return newOrderFeignClient.findNewOrderQueueData(driverId).getData();
    }
}
