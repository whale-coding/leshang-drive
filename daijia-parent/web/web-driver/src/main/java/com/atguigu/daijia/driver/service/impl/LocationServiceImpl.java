package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.map.client.LocationFeignClient;
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

    /**
     * 更新司机经纬度位置
     * @param updateDriverLocationForm 司机Id及经纬度信息
     * @return 是否更新成功
     */
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        // 远程调用
        return locationFeignClient.updateDriverLocation(updateDriverLocationForm).getData();
    }
}
