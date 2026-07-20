package com.atguigu.daijia.customer.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-customer")
public interface CustomerInfoFeignClient {
    /**
     * 小程序授权登录
     * @param code 前端wx.login()获取的临时登录凭证
     * @return 登录用户的id
     */
    @GetMapping("/customer/info/login/{code}")
    public Result<Long> login(@PathVariable String code);

    /**
     * 获取客户登录信息
     * @param customerId 客户id
     * @return CustomerLoginVo
     */
    @GetMapping("/customer/info/getCustomerLoginInfo/{customerId}")
    Result<CustomerLoginVo> getCustomerLoginInfo(@PathVariable("customerId") Long customerId);

    /**
     * 更新客户微信手机号码
     * @param updateWxPhoneForm 更新微信手机号表单
     * @return 是否更新成功
     */
    @PostMapping("/customer/info/updateWxPhoneNumber")
    Result<Boolean> updateWxPhoneNumber(@RequestBody UpdateWxPhoneForm updateWxPhoneForm);

}