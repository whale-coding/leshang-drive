package com.atguigu.daijia.customer.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 微信小程序SDK Bean配置类
 * 基于WxJava框架初始化WxMaService核心操作对象
 * 提供微信小程序接口调用能力，例如：获取用户OpenId、登录校验等
 */
@Component
public class WxConfigOperator {

    @Resource
    private WxConfigProperties wxConfigProperties;  // 小程序配置属性

    /**
     * 注册微信小程序操作服务Bean
     * 组装小程序凭证配置，生成WxMaService实例交给Spring容器管理
     * 注入该Bean即可调用微信小程序各类开放API，最常用：登录获取用户OpenId
     * @return WxMaService 微信小程序核心操作对象
     */
    @Bean
    public WxMaService wxMaService(){
        // 初始化小程序默认配置实现类,设置小程序AppID、AppSecret密钥
        WxMaDefaultConfigImpl wxMaConfig = new WxMaDefaultConfigImpl();
        wxMaConfig.setAppid(wxConfigProperties.getAppId());
        wxMaConfig.setSecret(wxConfigProperties.getSecret());

        // 创建小程序服务实现对象，绑定配置信息
        WxMaService wxMaService = new WxMaServiceImpl();
        wxMaService.setWxMaConfig(wxMaConfig);

        // 返回微信小程序核心操作对象
        return wxMaService;
    }
}
