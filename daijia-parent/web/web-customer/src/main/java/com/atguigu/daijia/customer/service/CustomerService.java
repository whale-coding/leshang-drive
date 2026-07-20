package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;

public interface CustomerService {

    // 微信小程序授权登录
    String login(String code);

    // 获取客户登录信息
    CustomerLoginVo getCustomerLoginInfo(long customerId);

    // 更新用户微信手机号
    Object updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm);
}
