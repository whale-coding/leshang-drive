package com.atguigu.daijia.dispatch.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@FeignClient(value = "service-dispatch")
public interface NewOrderFeignClient {

    /**
     * 添加新订单任务
     * @param newOrderDispatchVo 新订单任务VO
     * @return 任务ID
     */
    @PostMapping("/dispatch/newOrder/addAndStartTask")
    Result<Long> addAndStartTask(@RequestBody NewOrderTaskVo newOrderDispatchVo);

    /**
     * 查询司机新订单数据
     *
     * @param driverId 司机ID
     * @return 新订单数据
     */
    @GetMapping("/dispatch/newOrder/findNewOrderQueueData/{driverId}")
    Result<List<NewOrderDataVo>> findNewOrderQueueData(@PathVariable Long driverId);

    /**
     * 清空新订单队列数据
     * @param driverId 司机ID
     * @return 是否清空成功
     */
    @GetMapping("/dispatch/newOrder/clearNewOrderQueueData/{driverId}")
    Result<Boolean> clearNewOrderQueueData(@PathVariable Long driverId);
}