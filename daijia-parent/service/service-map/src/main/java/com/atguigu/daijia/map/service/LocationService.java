package com.atguigu.daijia.map.service;

import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;

public interface LocationService {
    // 更新司机经纬度位置
    Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);

    // 删除司机经纬度位置
    Boolean removeDriverLocation(Long driverId);
}
