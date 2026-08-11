package com.atguigu.daijia.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.constant.DriverConstant;
import com.atguigu.daijia.driver.mapper.DriverAccountMapper;
import com.atguigu.daijia.driver.mapper.DriverInfoMapper;
import com.atguigu.daijia.driver.mapper.DriverLoginLogMapper;
import com.atguigu.daijia.driver.mapper.DriverSetMapper;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.DriverAccount;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.entity.driver.DriverLoginLog;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.CreatePersonRequest;
import com.tencentcloudapi.iai.v20200303.models.CreatePersonResponse;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverInfoService {

    @Resource
    private DriverInfoMapper driverInfoMapper;

    @Resource
    private DriverAccountMapper driverAccountMapper;

    @Resource
    private WxMaService wxMaService;  // 微信操作对象

    @Resource
    private DriverSetMapper driverSetMapper;

    @Resource
    private DriverLoginLogMapper driverLoginLogMapper;

    @Resource
    private CosService cosService;

    @Resource
    private TencentCloudProperties tencentCloudProperties;

    /**
     * 司机端登录
     * @param code 前端wx.login()获取的临时登录凭证
     * @return 司机id
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long login(String code) {
        // 校验入参code非空
        if (code == null || code.trim().isEmpty()) {
            log.error("小程序登录code参数为空");
            throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }

        String openid = null;
        try {
            // 1.根据code，使用微信操作对象，获取微信唯一标识openid
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            openid = sessionInfo.getOpenid();

            log.info("【小程序授权】openid={}", openid);
        } catch (Exception e) {
            log.error("小程序code换取openid失败，code:{}，异常:{}", code, e.getMessage(), e);
            throw new GuiguException(ResultCodeEnum.WX_CODE_ERROR);
        }

        // 2.根据openid查询数据库表，判断是否第一次登录
        LambdaQueryWrapper<DriverInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DriverInfo::getWxOpenId,openid);
        DriverInfo driverInfo = driverInfoMapper.selectOne(queryWrapper);  // openid是唯一的，最多只有一条记录

        // 3.如果是第一次登录，添加信息到用户表（注册）
        if(driverInfo == null){
            // 司机基本信息
            driverInfo = new DriverInfo();
            driverInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            driverInfo.setAvatarUrl(DriverConstant.DEFAULT_DRIVER_AVATAR);
            driverInfo.setWxOpenId(openid);
            // 插入数据库表
            driverInfoMapper.insert(driverInfo);

            // 4.初始化默认设置
            DriverSet driverSet = new DriverSet();
            driverSet.setDriverId(driverInfo.getId());
            driverSet.setOrderDistance(new BigDecimal(0)); // 0：无限制
            driverSet.setAcceptDistance(new BigDecimal(SystemConstant.ACCEPT_DISTANCE)); // 默认接单范围：5公里
            driverSet.setIsAutoAccept(0); // 0：否 1：是
            driverSetMapper.insert(driverSet);

            // 5.初始化司机账户
            DriverAccount driverAccount = new DriverAccount();
            driverAccount.setDriverId(driverInfo.getId());
            driverAccountMapper.insert(driverAccount);
        }

        // 6.记录登录日志信息
        DriverLoginLog driverLoginLog = new DriverLoginLog();
        driverLoginLog.setDriverId(driverInfo.getId());
        driverLoginLog.setMsg("小程序登录");
        driverLoginLogMapper.insert(driverLoginLog);

        // 7.返回司机id
        return driverInfo.getId();
    }

    /**
     * 获取司机登录信息
     * @param driverId 司机id
     * @return DriverLoginVo
     */
    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        // 查询司机基本信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        // 封装返回结果VO
        DriverLoginVo driverLoginVo = new DriverLoginVo();
        BeanUtils.copyProperties(driverInfo, driverLoginVo);
        // 是否创建人脸库人员，接单时做人脸识别判断
        Boolean isArchiveFace = StringUtils.hasText(driverInfo.getFaceModelId());
        driverLoginVo.setIsArchiveFace(isArchiveFace);

        return driverLoginVo;
    }

    /**
     * 获取司机认证信息
     * @param driverId 司机id
     * @return DriverAuthInfoVo 司机认证信息封装VO
     */
    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        // 查询认证信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);

        // 封装DriverAuthInfoVo
        DriverAuthInfoVo driverAuthInfoVo = new DriverAuthInfoVo();
        BeanUtils.copyProperties(driverInfo, driverAuthInfoVo);

        // 设置身份证、驾驶证等照片的地址（上传之后生成临时地址,用于前端回显）
        driverAuthInfoVo.setIdcardBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardBackUrl()));
        driverAuthInfoVo.setIdcardFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardFrontUrl()));
        driverAuthInfoVo.setIdcardHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardHandUrl()));
        driverAuthInfoVo.setDriverLicenseFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseFrontUrl()));
        driverAuthInfoVo.setDriverLicenseBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseBackUrl()));
        driverAuthInfoVo.setDriverLicenseHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseHandUrl()));

        return driverAuthInfoVo;
    }

    /**
     * 更新司机认证信息
     * @param updateDriverAuthInfoForm 更新认证信息表单
     * @return 是否更新成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        DriverInfo driverInfo = new DriverInfo();
        driverInfo.setId(updateDriverAuthInfoForm.getDriverId());  // 设置司机id
        BeanUtils.copyProperties(updateDriverAuthInfoForm, driverInfo);

        return this.updateById(driverInfo);
    }

    /**
     * 创建司机人脸模型
     * @param driverFaceModelForm 司机人脸模型表单，包括司机id和人脸图
     * @return 是否创建成功
     * 文档地址：https://cloud.tencent.com/document/api/867/45014
     */
    @Override
    @SneakyThrows
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        // 查询司机信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverFaceModelForm.getDriverId());

        // 构建腾讯云凭证对象，使用配置文件中的SecretId、SecretKey
        Credential cred = new Credential(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());

        // 创建腾讯云人脸识别客户端
        IaiClient client = getIaiClient(cred);

        // 实例化一个请求对象，每个接口都会对应一个request对象
        CreatePersonRequest req = new CreatePersonRequest();
        req.setGroupId(tencentCloudProperties.getPersonGroupId());
        // 设置基本信息
        req.setPersonId(String.valueOf(driverInfo.getId()));
        req.setGender(Long.parseLong(driverInfo.getGender()));
        req.setQualityControl(4L);
        req.setUniquePersonControl(4L);
        req.setPersonName(driverInfo.getName());
        req.setImage(driverFaceModelForm.getImageBase64());

        // 执行接口调用，返回原始创建结果，返回的resp是一个CreatePersonResponse的实例，与请求对象对应
        CreatePersonResponse resp = client.CreatePerson(req);

        // 原始响应JSON日志，便于排查识别异常
        log.info(CreatePersonResponse.toJsonString(resp));

        // 如果接口调用返回有内容，则保存人脸模型ID
        if (StringUtils.hasText(resp.getFaceId())){
            // 人脸校验必要参数，保存到数据库表
            driverInfo.setFaceModelId(resp.getFaceId());
            driverInfoMapper.updateById(driverInfo);
        }

        return true;
    }

    /**
     * 创建腾讯云人脸识别客户端对象
     * @param cred 腾讯云调用凭证
     * @return IaiClient 人脸识别客户端对象
     */
    private IaiClient getIaiClient(Credential cred) {
        // 配置HTTP请求参数(可选)
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("iai.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        // 实例化要请求产品(人脸识别)的client对象,clientProfile是可选的
        return new IaiClient(cred, tencentCloudProperties.getRegion(), clientProfile);
    }

    /**
     * 获取司机设置信息
     * @param driverId 司机Id
     * @return DriverSet 司机设置信息
     */
    @Override
    public DriverSet getDriverSet(Long driverId) {
        LambdaQueryWrapper<DriverSet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DriverSet::getDriverId, driverId);

        return driverSetMapper.selectOne(queryWrapper);
    }
}