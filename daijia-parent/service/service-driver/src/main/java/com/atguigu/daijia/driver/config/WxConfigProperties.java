package com.atguigu.daijia.driver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置属性映射类
 * 读取application.yml中前缀 wx.miniapp 的配置信息
 * 封装小程序AppId、AppSecret凭证，供WxMaService初始化使用
 */
@Data
@Component
@ConfigurationProperties("wx.miniapp")
public class WxConfigProperties {
    private String appId;  // 微信小程序唯一标识AppID
    private String secret;  // # 微信小程序应用密钥AppSecret
}
