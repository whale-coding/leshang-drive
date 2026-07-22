package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.client.CosFeignClient;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    @Resource
    private CosFeignClient cosFeignClient;

    /**
     * 文件上传
     * @param file 要上传的文件
     * @param path 文件存储的路径
     * @return CosUploadVo
     */
    @Override
    public CosUploadVo upload(MultipartFile file, String path) {
        // 远程调用，上传文件
        return cosFeignClient.upload(file, path).getData();
    }
}
