package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerController {
    @Resource
    private CustomerService customerInfoService;

    @Resource
    private RedisTemplate redisTemplate;

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> wxLogin(@PathVariable String code) {
        return Result.ok(customerInfoService.login(code));
    }

    @Operation(summary = "获取客户登录信息")
    @GetMapping("/getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfo(@RequestHeader(value="token") String token) {
        // 根据请求头token从redis中获取用户id
        String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
        Object customerIdObj = redisTemplate.opsForValue().get(loginKey);

        // token不存在/已过期
        if (customerIdObj == null) {
            log.warn("获取登录信息失败，token无效或已过期，token:{}", token);
            throw new GuiguException(ResultCodeEnum.TOKEN_INVALID);
        }
        String customerIdStr = String.valueOf(customerIdObj);

        // customerId类型转换为Long
        long customerId;
        try {
            customerId = Long.parseLong(customerIdStr);
        } catch (NumberFormatException e) {
            log.error("Redis中用户登录ID格式非法，key:{}, value:{}", loginKey, customerIdStr, e);
            throw new GuiguException(ResultCodeEnum.TOKEN_INVALID);
        }
        log.info("获取登录用户信息成功，customerId:{}", customerId);

        // 调用service查询登录用户信息
        CustomerLoginVo loginVo = customerInfoService.getCustomerLoginInfo(customerId);
        return Result.ok(loginVo);
    }
}

