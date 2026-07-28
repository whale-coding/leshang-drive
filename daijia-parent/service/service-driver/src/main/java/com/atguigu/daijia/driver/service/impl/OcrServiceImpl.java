package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.driver.service.OcrService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import com.atguigu.daijia.model.vo.driver.DriverLicenseOcrVo;
import com.atguigu.daijia.model.vo.driver.IdCardOcrVo;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ocr.v20181119.OcrClient;
import com.tencentcloudapi.ocr.v20181119.models.*;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.format.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OcrServiceImpl implements OcrService {

    @Resource
    private CosService cosService;

    @Resource
    private TencentCloudProperties tencentCloudProperties;

    /**
     * 身份证OCR识别
     * 调用腾讯云OCR接口识别身份证正反面信息，并将图片上传至腾讯云COS存储
     * @param file 身份证照片文件
     * @return IdCardOcrVo
     * 文档地址：https://cloud.tencent.com/document/product/866/33524
     */
    @Override
    @SneakyThrows
    public IdCardOcrVo idCardOcr(MultipartFile file) {
        // 将图片字节数组进行Base64编码，用于腾讯云OCR接口传参
        byte[] encoder = Base64.encodeBase64(file.getBytes());
        String idCardBase64 = new String(encoder);

        // 构建腾讯云凭证对象，使用配置文件中的SecretId、SecretKey
        Credential cred = new Credential(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());

        // 调用腾讯云身份证识别接口，获取识别响应结果
        IDCardOCRResponse resp = getIdCardOCRResponse(cred, idCardBase64);

        // 打印OCR原始响应JSON日志，便于排查识别异常
        log.info(IDCardOCRResponse.toJsonString(resp));

        // 初始化返回结果VO
        IdCardOcrVo idCardOcrVo = new IdCardOcrVo();

        // 判断身份证正反面：存在姓名字段代表身份证正面（人像面）
        if (StringUtils.hasText(resp.getName())) {
            // 身份证正面
            // 身份证正面（人像面）信息填充
            idCardOcrVo.setName(resp.getName());
            idCardOcrVo.setGender("男".equals(resp.getSex()) ? "1" : "2");  // 性别转换：男=1，女=2
            idCardOcrVo.setBirthday(DateTimeFormat.forPattern("yyyy/MM/dd").parseDateTime(resp.getBirth()).toDate());  // 出生日期字符串转为Date对象
            idCardOcrVo.setIdcardNo(resp.getIdNum());
            idCardOcrVo.setIdcardAddress(resp.getAddress());

            // 上传身份证正面图片到腾讯云cos
            CosUploadVo cosUploadVo = cosService.upload(file, "idCard");
            idCardOcrVo.setIdcardFrontUrl(cosUploadVo.getUrl());
            idCardOcrVo.setIdcardFrontShowUrl(cosUploadVo.getShowUrl());
        } else {
            // 身份证反面（国徽面）信息填充
            // 证件有效期："2010.07.21-2020.07.21"
            String idcardExpireString = resp.getValidDate().split("-")[1];
            idCardOcrVo.setIdcardExpire(DateTimeFormat.forPattern("yyyy.MM.dd").parseDateTime(idcardExpireString).toDate());
            // 上传身份证反面图片到腾讯云cos
            CosUploadVo cosUploadVo = cosService.upload(file, "idCard");
            idCardOcrVo.setIdcardBackUrl(cosUploadVo.getUrl());
            idCardOcrVo.setIdcardBackShowUrl(cosUploadVo.getShowUrl());
        }

        // 返回vo
        return idCardOcrVo;
    }

    /**
     * 驾驶证OCR识别
     * 调用腾讯云OCR接口识别驾驶证正反面信息，并将图片上传至腾讯云COS存储
     * @param file 驾驶证照片文件
     * @return DriverLicenseOcrVo 识别结果封装VO
     * 文档地址：https://cloud.tencent.com/document/product/866/36213
     */
    @Override
    @SneakyThrows
    public DriverLicenseOcrVo driverLicenseOcr(MultipartFile file) {
        // 将图片字节数组进行Base64编码，用于腾讯云OCR接口传参
        byte[] encoder = Base64.encodeBase64(file.getBytes());
        String driverLicenseBase64 = new String(encoder);

        // 构建腾讯云凭证对象，使用配置文件中的SecretId、SecretKey
        Credential cred = new Credential(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());

        // 调用腾讯云身份证识别接口，获取识别响应结果
        DriverLicenseOCRResponse resp = getDriverLicenseOCRResponse(cred, driverLicenseBase64);

        // 打印OCR原始响应JSON日志，便于排查识别异常
        log.info(VehicleLicenseOCRResponse.toJsonString(resp));

        // 初始化返回结果VO
        DriverLicenseOcrVo driverLicenseOcrVo = new DriverLicenseOcrVo();

        // 判断驾驶证正反面：存在姓名字段代表驾驶证正面
        if (StringUtils.hasText(resp.getName())){
            // 驾驶证正面信息填充
            driverLicenseOcrVo.setName(resp.getName());  // 持证人姓名，业务上建议与身份证姓名做一致性校验
            driverLicenseOcrVo.setDriverLicenseClazz(resp.getClass_());  // 准驾车型
            driverLicenseOcrVo.setDriverLicenseNo(resp.getCardCode());  // 驾驶证号码
            driverLicenseOcrVo.setDriverLicenseIssueDate(DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(resp.getDateOfFirstIssue()).toDate());  // 初次领证日期
            driverLicenseOcrVo.setDriverLicenseExpire(DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(resp.getEndDate()).toDate());  // 证件有效截止日期

            // 将驾驶证正面原图上传至COS，保存资源地址
            CosUploadVo cosUploadVo = cosService.upload(file, "driverLicense");
            driverLicenseOcrVo.setDriverLicenseFrontUrl(cosUploadVo.getUrl());
            driverLicenseOcrVo.setDriverLicenseFrontShowUrl(cosUploadVo.getShowUrl());
        }else {
            // 驾驶证反面信息填充
            // 将驾驶证反面原图上传至COS，保存资源地址
            CosUploadVo cosUploadVo =  cosService.upload(file, "driverLicense");
            driverLicenseOcrVo.setDriverLicenseBackUrl(cosUploadVo.getUrl());
            driverLicenseOcrVo.setDriverLicenseBackShowUrl(cosUploadVo.getShowUrl());
        }

        // 返回vo
        return driverLicenseOcrVo;
    }

    /**
     * 发起腾讯云驾驶证OCR识别请求
     * @param cred 腾讯云调用凭证
     * @param driverLicenseBase64 驾驶证图片Base64编码字符串
     * @return DriverLicenseOCRResponse 腾讯云OCR原始响应对象
     * @throws TencentCloudSDKException 腾讯云SDK调用异常
     */
    private DriverLicenseOCRResponse getDriverLicenseOCRResponse(Credential cred, String driverLicenseBase64) throws TencentCloudSDKException {
        // 配置HTTP请求参数(可选)
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("ocr.tencentcloudapi.com");  // 接口请求域名
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        // 初始化OCR客户端
        OcrClient client = new OcrClient(cred, tencentCloudProperties.getRegion(), clientProfile);

        // 组装识别请求参数，传入图片base64
        DriverLicenseOCRRequest req = new DriverLicenseOCRRequest();
        req.setImageBase64(driverLicenseBase64);

        // 执行接口调用，返回原始识别结果
        return client.DriverLicenseOCR(req);
    }

    /**
     * 发起腾讯云身份证OCR识别请求
     * @param cred 腾讯云调用凭证
     * @param idCardBase64 身份证图片Base64编码字符串
     * @return 腾讯云OCR原始响应对象
     * @throws TencentCloudSDKException 腾讯云SDK调用异常
     */
    private IDCardOCRResponse getIdCardOCRResponse(Credential cred, String idCardBase64) throws TencentCloudSDKException {
        // 配置HTTP请求参数(可选)
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("ocr.tencentcloudapi.com");  // 接口请求域名
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        // 初始化OCR客户端
        OcrClient client = new OcrClient(cred, tencentCloudProperties.getRegion(), clientProfile);

        // 组装识别请求参数，传入图片base64
        IDCardOCRRequest req = new IDCardOCRRequest();
        req.setImageBase64(idCardBase64);

        // 执行接口调用，返回原始识别结果
        return client.IDCardOCR(req);
    }
}
