package com.atguigu.daijia.dispatch.xxl.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 测试XXL-Job的使用
 */
@Slf4j
@Component
public class DispatchJobHandler {

    @XxlJob("firstJobHandler")
    public void firstJobHandler() {
        log.info("xxl-job项目集成测试");
    }
}