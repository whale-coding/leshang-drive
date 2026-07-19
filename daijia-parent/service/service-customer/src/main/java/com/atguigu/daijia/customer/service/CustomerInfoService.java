package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface CustomerInfoService extends IService<CustomerInfo> {
    // 微信小程序授权登录
    Long login(String code);

    // 获取客户登录信息
    CustomerLoginVo getCustomerLoginInfo(Long customerId);
}
