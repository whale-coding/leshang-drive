package com.atguigu.daijia.dispatch.xxl.client;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.xxl.config.XxlJobClientConfig;
import com.atguigu.daijia.model.entity.dispatch.XxlJobInfo;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class XxlJobClient {

    @Resource
    private XxlJobClientConfig xxlJobClientConfig;  // xxl-job客户端配置

    @Resource
    private RestTemplate restTemplate;  // 远程调用

    @SneakyThrows
    public Long addJob(String executorHandler, String param, String corn, String desc){
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setJobGroup(xxlJobClientConfig.getJobGroupId());
        xxlJobInfo.setJobDesc(desc);
        xxlJobInfo.setAuthor("qy");
        xxlJobInfo.setScheduleType("CRON");
        xxlJobInfo.setScheduleConf(corn);
        xxlJobInfo.setGlueType("BEAN");
        xxlJobInfo.setExecutorHandler(executorHandler);
        xxlJobInfo.setExecutorParam(param);
        xxlJobInfo.setExecutorRouteStrategy("FIRST");
        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        xxlJobInfo.setMisfireStrategy("FIRE_ONCE_NOW");
        xxlJobInfo.setExecutorTimeout(0);
        xxlJobInfo.setExecutorFailRetryCount(0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getAddUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        JSONObject body = response.getBody();
        if(body != null && response.getStatusCode().value() == 200 && body.getIntValue("code") == 200) {
            log.info("增加xxl执行任务成功,返回信息:{}", body.toJSONString());
            //content为任务id
            return body.getLong("content");
        }
        log.info("调用xxl增加执行任务失败,HTTP状态码:{},返回信息:{}", response.getStatusCode(), body);
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

    public Boolean startJob(Long jobId) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId.intValue());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getStartJobUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        JSONObject body = response.getBody();
        if(body != null && response.getStatusCode().value() == 200 && body.getIntValue("code") == 200) {
            log.info("启动xxl执行任务成功:{},返回信息:{}", jobId, body.toJSONString());
            return true;
        }
        log.info("启动xxl执行任务失败:{},HTTP状态码:{},返回信息:{}", jobId, response.getStatusCode(), body);
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

    public Boolean stopJob(Long jobId) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId.intValue());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getStopJobUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        JSONObject body = response.getBody();
        if(body != null && response.getStatusCode().value() == 200 && body.getIntValue("code") == 200) {
            log.info("停止xxl执行任务成功:{},返回信息:{}", jobId, body.toJSONString());
            return true;
        }
        log.info("停止xxl执行任务失败:{},HTTP状态码:{},返回信息:{}", jobId, response.getStatusCode(), body);
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

    public Boolean removeJob(Long jobId) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId.intValue());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getRemoveUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        JSONObject body = response.getBody();
        if(body != null && response.getStatusCode().value() == 200 && body.getIntValue("code") == 200) {
            log.info("删除xxl执行任务成功:{},返回信息:{}", jobId, body.toJSONString());
            return true;
        }
        log.info("删除xxl执行任务失败:{},HTTP状态码:{},返回信息:{}", jobId, response.getStatusCode(), body);
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

    public Long addAndStart(String executorHandler, String param, String corn, String desc) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setJobGroup(xxlJobClientConfig.getJobGroupId());
        xxlJobInfo.setJobDesc(desc);
        xxlJobInfo.setAuthor("qy");
        xxlJobInfo.setScheduleType("CRON");
        xxlJobInfo.setScheduleConf(corn);
        xxlJobInfo.setGlueType("BEAN");
        xxlJobInfo.setExecutorHandler(executorHandler);
        xxlJobInfo.setExecutorParam(param);
        xxlJobInfo.setExecutorRouteStrategy("FIRST");
        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        xxlJobInfo.setMisfireStrategy("FIRE_ONCE_NOW");
        xxlJobInfo.setExecutorTimeout(0);
        xxlJobInfo.setExecutorFailRetryCount(0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getAddAndStartUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        JSONObject body = response.getBody();
        if(body != null && response.getStatusCode().value() == 200 && body.getIntValue("code") == 200) {
            log.info("增加并开始执行xxl任务成功,返回信息:{}", body.toJSONString());
            // content为任务id
            return body.getLong("content");
        }
        log.info("增加并开始执行xxl任务失败,HTTP状态码:{},返回信息:{}", response.getStatusCode(), body);
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }
}
