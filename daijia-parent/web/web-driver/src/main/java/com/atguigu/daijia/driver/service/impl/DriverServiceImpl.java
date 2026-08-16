package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {

    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private LocationFeignClient locationFeignClient;

    @Resource
    private NewOrderFeignClient newOrderDispatchFeignClient;

    /**
     * 司机端登录
     * @param code 前端wx.login()获取的临时登录凭证
     * @return 司机id
     */
    @Override
    public String login(String code) {
        // 远程调用，获取openId
        Long driverId = driverInfoFeignClient.login(code).getData();

        String token = UUID.randomUUID().toString().replace("-", "");
        // 将司机id存入redis key:token value:司机id
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                driverId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);

        // 返回token
        return token;
    }

    /**
     * 获取司机登录信息
     * @param driverId 司机id
     * @return DriverLoginVo
     */
    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        // 远程调用，获取司机登录信息
        return driverInfoFeignClient.getDriverLoginInfo(driverId).getData();
    }

    /**
     * 获取司机认证信息
     * @param driverId 司机id
     * @return DriverAuthInfoVo
     */
    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        // 远程调用
        return driverInfoFeignClient.getDriverAuthInfo(driverId).getData();
    }

    /**
     * 更新司机认证信息
     * @param updateDriverAuthInfoForm 更新认证信息表单
     * @return 是否更新成功
     */
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        // 远程调用
        return driverInfoFeignClient.UpdateDriverAuthInfo(updateDriverAuthInfoForm).getData();
    }

    /**
     * 创建司机人脸模型
     * @param driverFaceModelForm 司机人脸模型表单
     * @return 是否创建成功
     */
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        // 远程调用
        return driverInfoFeignClient.creatDriverFaceModel(driverFaceModelForm).getData();
    }

    /**
     * 判断司机当日是否进行过人脸识别
     * @param driverId 司机ID
     * @return 是否进行过人脸识别
     */
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        // 远程调用
        return driverInfoFeignClient.isFaceRecognition(driverId).getData();
    }

    /**
     * 司机人脸识别
     * @param driverFaceModelForm 司机ID和人脸照片
     * @return 是否识别成功
     */
    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        // 远程调用
        return driverInfoFeignClient.verifyDriverFace(driverFaceModelForm).getData();
    }

    /**
     * 开始接单服务
     * @param driverId 司机ID
     * @return 是否成功
     */
    @Override
    public Boolean startService(Long driverId) {
        // 1.判断登录认证状态
        DriverLoginVo driverLoginVo = driverInfoFeignClient.getDriverLoginInfo(driverId).getData();
        if(driverLoginVo.getAuthStatus().intValue() != 2) {
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }

        // 2.判断当日是否通过人脸识别
        Boolean isFaceRecognition = driverInfoFeignClient.isFaceRecognition(driverId).getData();
        if(!isFaceRecognition) {
            throw new GuiguException(ResultCodeEnum.FACE_ERROR);
        }

        // 3.更新司机接单状态，"1"表示开始接单
        driverInfoFeignClient.updateServiceStatus(driverId, 1);

        // 4.删除redis中的司机位置信息
        locationFeignClient.removeDriverLocation(driverId);

        // 5.清空司机新订单临时队列数据
        newOrderDispatchFeignClient.clearNewOrderQueueData(driverId);

        return true;
    }

    /**
     * 停止接单服务
     * @param driverId 司机ID
     * @return 是否成功
     */
    @Override
    public Boolean stopService(Long driverId) {
        // 1.更新司机接单状态，"0"表示停止接单
        driverInfoFeignClient.updateServiceStatus(driverId, 0);

        // 2.删除redis中的司机位置信息
        locationFeignClient.removeDriverLocation(driverId);

        // 3.清空司机新订单临时队列数据
        newOrderDispatchFeignClient.clearNewOrderQueueData(driverId);

        return true;
    }
}
