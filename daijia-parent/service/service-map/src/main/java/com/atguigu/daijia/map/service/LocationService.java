package com.atguigu.daijia.map.service;

import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;

import java.util.List;

public interface LocationService {
    // 更新司机经纬度位置
    Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);

    // 删除司机经纬度位置
    Boolean removeDriverLocation(Long driverId);

    // 搜索附近满足条件的司机
    List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm);
}
