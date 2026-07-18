package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerServiceImpl implements CustomerService {

    // 注入远程调用接口
    @Resource
    private CustomerInfoFeignClient customerInfoFeignClient;

    @Resource
    private RedisTemplate redisTemplate;  // redis

    /**
     * 微信小程序授权登录
     * @param code 前端wx.login()获取的临时登录凭证
     * @return token
     */
    @Override
    public String login(String code) {
        // 1.远程调用，获取用户登录结果
        Result<Long> longResult = customerInfoFeignClient.login(code);

        // 2.判断是否登录失败（登录结果code !=200即失败，成功code返回的是200）
        if(longResult.getCode() != 200) {
            throw new GuiguException(longResult.getCode(), longResult.getMessage());
        }

        // 3.从远程调用结果中获取用户id
        Long customerId = longResult.getData();
        if(customerId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 4.生成token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 5.将用户id存到redis中  key:token  value:customerId
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX+token,
                customerId.toString(), RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);

        // 6.返回token
        return token;
    }
}
