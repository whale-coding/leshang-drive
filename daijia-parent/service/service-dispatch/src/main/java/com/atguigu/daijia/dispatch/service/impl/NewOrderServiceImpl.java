package com.atguigu.daijia.dispatch.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.dispatch.mapper.OrderJobMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.dispatch.xxl.client.XxlJobClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.dispatch.OrderJob;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderServiceImpl implements NewOrderService {

    @Resource
    private XxlJobClient xxlJobClient;  // xxl-job客户端

    @Resource
    private OrderJobMapper orderJobMapper;

    @Resource
    private LocationFeignClient locationFeignClient;

    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 添加并开始新订单任务调度
     * @param newOrderTaskVo 新订单任务VO
     * @return 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
        // 1.判断当前订单是否启动任务调度
        // 查询当前订单的任务
        OrderJob orderJob = orderJobMapper.selectOne(new LambdaQueryWrapper<OrderJob>()
                .eq(OrderJob::getOrderId, newOrderTaskVo.getOrderId()));
        // 2.订单没有调度任务的时候，我们才去使用XXL-JOB启动任务
        if(null == orderJob){
            // 创建并且启动调度任务:每1分钟执行一次，处理任务的bean为：newOrderTaskHandler
            Long jobId = xxlJobClient.addAndStart("newOrderTaskHandler", "", "0 0/1 * * * ?", "新订单任务,订单id："+newOrderTaskVo.getOrderId());

            // 记录订单与任务的关联信息
            orderJob = new OrderJob();
            orderJob.setOrderId(newOrderTaskVo.getOrderId());
            orderJob.setJobId(jobId);
            orderJob.setParameter(JSONObject.toJSONString(newOrderTaskVo));

            orderJobMapper.insert(orderJob);
        }
        // 返回任务ID
        return orderJob.getJobId();
    }

    /**
     * 执行XXL-JOB定时调度任务：搜索附近代驾司机
     * @param jobId 任务id
     */
    @Override
    public void executeTask(Long jobId) {
        // 1.根据jobId查询数据库，当前任务是否已经创建
        OrderJob orderJob = orderJobMapper.selectOne(new LambdaQueryWrapper<OrderJob>().eq(OrderJob::getJobId, jobId));
        if(null == orderJob) {
            // 没有任务没有创建，不往下执行
            return;
        }
        // 任务已经创建了
        // 获取OrderJob里面的对象
        NewOrderTaskVo newOrderTaskVo = JSONObject.parseObject(orderJob.getParameter(), NewOrderTaskVo.class);

        // 2.查询订单状态：如果当前订单为接单状态，继续执行；如果当前订单不是接单状态，停止任务调度
        // 查询订单状态，如果该订单还在接单状态，继续执行；如果不在接单状态，则停止定时调度
        Integer orderStatus = orderInfoFeignClient.getOrderStatus(newOrderTaskVo.getOrderId()).getData();
        if(orderStatus.intValue() != OrderStatus.WAITING_ACCEPT.getStatus().intValue()){
            // 不是接单状态，停止定时调度
            xxlJobClient.stopJob(jobId);
            log.info("停止任务调度: {}", JSON.toJSONString(newOrderTaskVo));
            return;
        }

        // 3.远程调用：搜索附近满足条件的可以接单的司机
        // 搜索附近满足条件的司机集合
        SearchNearByDriverForm searchNearByDriverForm = new SearchNearByDriverForm();
        searchNearByDriverForm.setLongitude(newOrderTaskVo.getStartPointLongitude());
        searchNearByDriverForm.setLatitude(newOrderTaskVo.getStartPointLatitude());
        searchNearByDriverForm.setMileageDistance(newOrderTaskVo.getExpectDistance());
        // 远程调用，获取满足可以接单司机集合
        List<NearByDriverVo> nearByDriverVoList = locationFeignClient.searchNearByDriver(searchNearByDriverForm).getData();

        // 5.遍历司机集合得到每个司机，为每个司机创建临时队列，存储新订单信息
        nearByDriverVoList.forEach(driver -> {
            // 记录司机id，防止重复推送订单信息
            String repeatKey = RedisConstant.DRIVER_ORDER_REPEAT_LIST+newOrderTaskVo.getOrderId();
            boolean isMember = redisTemplate.opsForSet().isMember(repeatKey, driver.getDriverId());  // Redis的Set防止重复
            // 不重复
            if(!isMember) {
                // 把订单信息推送给满足条件的多个司机
                // 记录该订单已放入司机临时容器
                redisTemplate.opsForSet().add(repeatKey, driver.getDriverId());
                // 过期时间：15分钟，新订单15分钟没人接单自动取消
                redisTemplate.expire(repeatKey, RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME, TimeUnit.MINUTES);

                NewOrderDataVo newOrderDataVo = new NewOrderDataVo();
                newOrderDataVo.setOrderId(newOrderTaskVo.getOrderId());
                newOrderDataVo.setStartLocation(newOrderTaskVo.getStartLocation());
                newOrderDataVo.setEndLocation(newOrderTaskVo.getEndLocation());
                newOrderDataVo.setExpectAmount(newOrderTaskVo.getExpectAmount());
                newOrderDataVo.setExpectDistance(newOrderTaskVo.getExpectDistance());
                newOrderDataVo.setExpectTime(newOrderTaskVo.getExpectTime());
                newOrderDataVo.setFavourFee(newOrderTaskVo.getFavourFee());
                newOrderDataVo.setDistance(driver.getDistance());
                newOrderDataVo.setCreateTime(newOrderTaskVo.getCreateTime());

                // 将消息保存到司机的临时队列里面，司机接单了会定时轮询到他的临时队列获取订单消息
                String key = RedisConstant.DRIVER_ORDER_TEMP_LIST+driver.getDriverId();
                redisTemplate.opsForList().leftPush(key, JSONObject.toJSONString(newOrderDataVo));  // Redis的List实现队列
                // 过期时间：1分钟，1分钟未消费，自动过期
                // 注：司机端开启接单，前端每5秒（远小于1分钟）拉取1次“司机临时队列”里面的新订单消息
                redisTemplate.expire(key, RedisConstant.DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME, TimeUnit.MINUTES);
                log.info("该新订单信息已放入司机临时队列: {}", JSON.toJSONString(newOrderDataVo));
            }
        });
    }

    /**
     * 查询司机新订单数据
     * @param driverId 司机ID
     * @return 新订单数据
     */
    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        List<NewOrderDataVo> list = new ArrayList<>();

        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        // 从Redis List（队列）中取数据
        long size = redisTemplate.opsForList().size(key);
        if(size > 0) {
            for(int i=0; i<size; i++) {
                String content = (String)redisTemplate.opsForList().leftPop(key);
                NewOrderDataVo newOrderDataVo = JSONObject.parseObject(content, NewOrderDataVo.class);
                list.add(newOrderDataVo);
            }
        }
        return list;
    }

    /**
     * 清空新订单队列数据
     * @param driverId 司机ID
     * @return 是否清空成功
     */
    @Override
    public Boolean clearNewOrderQueueData(Long driverId) {
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        // 直接删除，司机开启服务后，有新订单会自动创建容器
        redisTemplate.delete(key);
        return true;
    }
}
