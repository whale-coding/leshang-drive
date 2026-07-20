package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {

    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 司机端登录
     * @param code 前端wx.login()获取的临时登录凭证
     * @return 司机id
     */
    @Override
    public String login(String code) {
        // 远程调用，获取openId
        Long driverId = driverInfoFeignClient.login(code).getData();

        String token = UUID.randomUUID().toString().replace("-", "");
        // 将司机id存入redis key:token value:司机id
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                driverId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);

        // 返回token
        return token;
    }

    /**
     * 获取司机登录信息
     * @param driverId 司机id
     * @return DriverLoginVo
     */
    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        // 远程调用，获取司机登录信息
        return driverInfoFeignClient.getDriverLoginInfo(driverId).getData();
    }
}
