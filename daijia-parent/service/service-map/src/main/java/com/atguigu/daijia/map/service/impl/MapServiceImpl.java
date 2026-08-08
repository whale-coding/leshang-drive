package com.atguigu.daijia.map.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.map.service.MapService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements MapService {

    @Resource
    private RestTemplate restTemplate;  // 用于调用腾讯服务

    @Value("${tencent.map.key}")
    private String key;    // 腾讯地图服务

    /**
     * 计算驾驶线路
     * @param calculateDrivingLineForm 经纬度信息 表单
     * @return 驾驶路线方案
     * 参考API文档：https://lbs.qq.com/service/webService/webServiceGuide/route/webServiceRoute
     * 请求URL示例：https://apis.map.qq.com/ws/direction/v1/walking/?from=39.984042,116.307535&to=39.976249,116.316569&key=[你的key]
     */
    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        // 请求腾讯地图提供的接口，按照接口要求传递相关参数，返回需要的结果

        // 1.定义调用地址
        String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&key={key}";

        // 2.封装传递参数
        Map<String, String> map = new HashMap<>();
        // 开始位置: 经度,纬度
        map.put("from", calculateDrivingLineForm.getStartPointLatitude() + "," + calculateDrivingLineForm.getStartPointLongitude());
        // 结束位置: 经度,纬度
        map.put("to", calculateDrivingLineForm.getEndPointLatitude() + "," + calculateDrivingLineForm.getEndPointLongitude());
        // key
        map.put("key", key);

        // 3.使用restTemplate调用腾讯地图服务（GET请求）
        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);
        // 4.处理返回结果
        // 判断调用是否成功
        if(result.getIntValue("status") != 0) {
            throw new GuiguException(ResultCodeEnum.MAP_FAIL);
        }
        // 获取返回的路线方案（选取第一条路线，一般为最佳路线）
        JSONObject route = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);

        // 5.封装结果
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        // 方案总距离，单位:米,这里将其转化为km,保留2位小数（向上取整）
        drivingLineVo.setDistance(route.getBigDecimal("distance")
                .divide(new BigDecimal(1000))
                .setScale(2, RoundingMode.HALF_UP));
        // 方案预估时间，单位:分钟
        drivingLineVo.setDuration(route.getBigDecimal("duration"));
        // 方案路线坐标点串
        drivingLineVo.setPolyline(route.getJSONArray("polyline"));

        return drivingLineVo;
    }
}
