package com.atguigu.daijia.map.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;

    /**
     * 更新司机经纬度位置
     * @param updateDriverLocationForm 司机Id及经纬度信息
     * @return 是否更新成功
     */
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        // Redis GEO 主要用于存储地理位置信息，并对存储的信息进行相关操作。乘客下单后寻找5公里范围内开启接单服务的司机，通过Redis GEO进行计算。
        // 封装位置信息点
        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(), updateDriverLocationForm.getLatitude().doubleValue());

        // 把司机位置信息添加到redis的GEO中
        redisTemplate.opsForGeo().add(RedisConstant.DRIVER_GEO_LOCATION, point, updateDriverLocationForm.getDriverId().toString());
        return true;
    }

    /**
     * 删除司机经纬度位置
     * @param driverId 司机Id
     * @return 是否删除成功
     */
    @Override
    public Boolean removeDriverLocation(Long driverId) {
        redisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());
        return true;
    }

    /**
     * 搜索附近满足条件的司机
     * @param searchNearByDriverForm 搜索表单（经度、纬度、里程）
     * @return 满足条件的司机id和距离
     */
    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {
        // 搜索经纬度位置5公里以内的司机
        // 定义经纬度点
        Point point = new Point(searchNearByDriverForm.getLongitude().doubleValue(), searchNearByDriverForm.getLatitude().doubleValue());

        // 定义距离：5公里(系统配置)
        Distance distance = new Distance(SystemConstant.NEARBY_DRIVER_RADIUS, RedisGeoCommands.DistanceUnit.KILOMETERS);

        //定义以point点为中心，distance为距离这么一个范围（Circle对象）
        Circle circle = new Circle(point, distance);

        // 定义GEO参数
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance() // 包含距离
                .includeCoordinates() // 包含坐标
                .sortAscending(); // 排序：升序

        // 1.GEO RADIUS获取附近范围内的信息
        GeoResults<RedisGeoCommands.GeoLocation<String>> result = redisTemplate.opsForGeo()
                .radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args);

        // 2.收集信息，存入list
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = result.getContent();

        // 3.返回计算后的信息
        List<NearByDriverVo> list = new ArrayList();

        // 判空
        if(!CollectionUtils.isEmpty(content)){
            // 使用迭代器进行遍历
            Iterator<GeoResult<RedisGeoCommands.GeoLocation<String>>> iterator = content.iterator();
            while (iterator.hasNext()){
                GeoResult<RedisGeoCommands.GeoLocation<String>> item = iterator.next();

                // 司机id
                Long driverId = Long.parseLong(item.getContent().getName());
                // 当前距离
                BigDecimal currentDistance = new BigDecimal(item.getDistance().getValue()).setScale(2, RoundingMode.HALF_UP);

                log.info("司机：{}，距离：{}",driverId, item.getDistance().getValue());

                // 获取司机接单设置参数（远程调用）
                DriverSet driverSet = driverInfoFeignClient.getDriverSet(driverId).getData();
                // 接单里程判断，acceptDistance==0：不限制；接单里程 - 当前订单里程 < 0 不符合条件
                if(driverSet.getAcceptDistance().doubleValue() != 0 && driverSet.getAcceptDistance().subtract(currentDistance).doubleValue() < 0){
                    continue;
                }
                // 订单里程判断，orderDistance==0：不限制；接单距离 - 当前订单距离 < 0 不符合条件
                if(driverSet.getOrderDistance().doubleValue() != 0 && driverSet.getOrderDistance().subtract(searchNearByDriverForm.getMileageDistance()).doubleValue() < 0){
                    continue;
                }

                // 封装满足条件的附近司机信息
                NearByDriverVo nearByDriverVo = new NearByDriverVo();
                nearByDriverVo.setDriverId(driverId);
                nearByDriverVo.setDistance(currentDistance);
                // 添加到结果List中
                list.add(nearByDriverVo);
            }
        }
        // 返回符合条件是司机信息列表
        return list;
    }
}
