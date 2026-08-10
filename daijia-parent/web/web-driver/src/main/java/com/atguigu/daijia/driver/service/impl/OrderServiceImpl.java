package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;

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
}
