package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.client.OcrFeignClient;
import com.atguigu.daijia.driver.service.OcrService;
import com.atguigu.daijia.model.vo.driver.DriverLicenseOcrVo;
import com.atguigu.daijia.model.vo.driver.IdCardOcrVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OcrServiceImpl implements OcrService {

    @Resource
    private OcrFeignClient ocrFeignClient;

    /**
     * 身份证OCR识别
     * @param file 身份证照片文件
     * @return IdCardOcrVo
     */
    @Override
    public IdCardOcrVo idCardOcr(MultipartFile file) {
        // 远程调用
        return ocrFeignClient.idCardOcr(file).getData();
    }

    /**
     * 驾驶证OCR识别
     * @param file 驾驶证照片文件
     * @return DriverLicenseOcrVo
     */
    @Override
    public DriverLicenseOcrVo driverLicenseOcr(MultipartFile file) {
        // 远程调用
        return ocrFeignClient.driverLicenseOcr(file).getData();
    }
}
