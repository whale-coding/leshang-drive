package com.atguigu.daijia.customer.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.constant.CustomerConstant;
import com.atguigu.daijia.customer.mapper.CustomerInfoMapper;
import com.atguigu.daijia.customer.mapper.CustomerLoginLogMapper;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.entity.customer.CustomerLoginLog;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {

    @Resource
    private WxMaService wxMaService;  // 微信操作对象

    @Resource
    private CustomerInfoMapper customerInfoMapper;

    @Resource
    private CustomerLoginLogMapper customerLoginLogMapper;


    /**
     * 微信小程序授权登录
     * @param code 前端wx.login()获取的临时登录凭证
     * @return 登录用户的id
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 全部异常触发事务回滚，保证用户+登录日志同时成功/失败
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
        }catch (Exception e){
            log.error("小程序code换取openid失败，code:{}，异常:{}", code, e.getMessage(), e);
            throw new GuiguException(ResultCodeEnum.WX_CODE_ERROR);
        }

        // 2.根据openid查询数据库表，判断是否第一次登录
        LambdaQueryWrapper<CustomerInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomerInfo::getWxOpenId,openid);
        CustomerInfo customerInfo = customerInfoMapper.selectOne(queryWrapper);  // openid是唯一的，最多只有一条记录

        // 3.如果是第一次登录，添加信息到用户表（注册）
        if(customerInfo == null){
            customerInfo = new CustomerInfo();
            customerInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            customerInfo.setAvatarUrl(CustomerConstant.DEFAULT_CUSTOMER_AVATAR);
            customerInfo.setWxOpenId(openid);

            customerInfoMapper.insert(customerInfo);
        }

        // 4.记录登录日志信息
        CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        customerLoginLog.setCustomerId(customerInfo.getId());
        customerLoginLog.setMsg("小程序登录");
        customerLoginLogMapper.insert(customerLoginLog);

        // 5.返回用户id
        return customerInfo.getId();
    }

    /**
     * 获取客户登录信息
     * @param customerId 客户id
     * @return CustomerLoginVo
     */
    @Override
    public CustomerLoginVo getCustomerLoginInfo(Long customerId) {
        // 根据用户id查询用户信息
        CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);
        // 将用户信息封装成CustomerLoginVo
        CustomerLoginVo customerLoginVo = new CustomerLoginVo();
        BeanUtils.copyProperties(customerInfo, customerLoginVo);
        // 处理isBindPhone属性
        String phone = customerInfo.getPhone();
        boolean isBindPhone = StringUtils.hasText(phone);
        customerLoginVo.setIsBindPhone(isBindPhone);

        // 返回CustomerLoginVo
        return customerLoginVo;
    }

    /**
     * 更新客户微信手机号码
     * @param updateWxPhoneForm 更新微信手机号表单
     * @return 是否更新成功
     */
    @SneakyThrows
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        // 调用微信 API 获取用户的手机号
        WxMaPhoneNumberInfo phoneInfo = wxMaService.getUserService().getPhoneNoInfo(updateWxPhoneForm.getCode());
        String phoneNumber = phoneInfo.getPhoneNumber();
        log.info("phoneInfo:{}", JSON.toJSONString(phoneInfo));

        // 组装更新对象
        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setId(updateWxPhoneForm.getCustomerId());
        customerInfo.setPhone(phoneNumber);
        // 执行更新，返回受影响行数 int
        int result = customerInfoMapper.updateById(customerInfo);
        // int 转 Boolean：更新行数大于0代表成功
        return result > 0;
    }
}
