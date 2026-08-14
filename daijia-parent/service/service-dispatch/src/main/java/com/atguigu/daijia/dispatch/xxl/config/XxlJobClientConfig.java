package com.atguigu.daijia.dispatch.xxl.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * XXL-JOB 客户端配置类
 * 读取配置文件中调用的xxl-job调度中心任务操作的方法
 */
@Data
@Component
@ConfigurationProperties(prefix = "xxl.job.client")
public class XxlJobClientConfig {
    private Integer jobGroupId;
    private String addUrl;
    private String removeUrl;
    private String startJobUrl;
    private String stopJobUrl;
    private String addAndStartUrl;
}