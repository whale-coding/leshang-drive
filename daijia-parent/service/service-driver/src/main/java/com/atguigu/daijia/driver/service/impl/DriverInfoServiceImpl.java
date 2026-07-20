package com.atguigu.daijia.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.constant.DriverConstant;
import com.atguigu.daijia.driver.mapper.DriverAccountMapper;
import com.atguigu.daijia.driver.mapper.DriverInfoMapper;
import com.atguigu.daijia.driver.mapper.DriverLoginLogMapper;
import com.atguigu.daijia.driver.mapper.DriverSetMapper;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.DriverAccount;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.entity.driver.DriverLoginLog;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
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
}