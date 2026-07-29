package com.atguigu.daijia.driver.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-driver")
public interface DriverInfoFeignClient {
    /**
     * 司机端登录
     * @param code 临时登录凭证
     * @return Result<Long>
     */
    @GetMapping("/driver/info/login/{code}")
    Result<Long> login(@PathVariable String code);

    /**
     * 获取司机登录信息
     * @param driverId 司机id
     * @return Result<DriverLoginVo>
     */
    @GetMapping("/driver/info/getDriverLoginInfo/{driverId}")
    Result<DriverLoginVo> getDriverLoginInfo(@PathVariable Long driverId);

    /**
     * 获取司机认证信息
     * @param driverId 司机id
     * @return DriverAuthInfoVo
     */
    @GetMapping("/driver/info/getDriverAuthInfo/{driverId}")
    Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable Long driverId);

    /**
     * 更新司机认证信息
     * @param updateDriverAuthInfoForm 更新认证信息表单
     * @return 是否更新成功
     */
    @PostMapping("/driver/info/updateDriverAuthInfo")
    Result<Boolean> UpdateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm);
}