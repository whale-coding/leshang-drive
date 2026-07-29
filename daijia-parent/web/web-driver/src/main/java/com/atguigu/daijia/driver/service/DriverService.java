package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;

public interface DriverService {
    // 司机端登录
    String login(String code);

    // 获取司机登录信息
    DriverLoginVo getDriverLoginInfo(Long driverId);

    // 获取司机认证信息
    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    // 更新司机认证信息
    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);
}
