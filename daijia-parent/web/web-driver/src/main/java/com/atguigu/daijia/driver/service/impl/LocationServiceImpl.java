package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    @Resource
    private LocationFeignClient locationFeignClient;

    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;

    /**
     * 更新司机经纬度位置
     * @param updateDriverLocationForm 司机Id及经纬度信息
     * @return 是否更新成功
     */
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        // 获取司机设置信息:开启接单了才能更新司机接单位置
        DriverSet driverSet = driverInfoFeignClient.getDriverSet(updateDriverLocationForm.getDriverId()).getData();
        // 判断是否开始接单
        if(driverSet.getServiceStatus() == 1) {
            // 远程调用
            return locationFeignClient.updateDriverLocation(updateDriverLocationForm).getData();
        } else {
            throw new GuiguException(ResultCodeEnum.NO_START_SERVICE);
        }
    }
}
