package com.atguigu.daijia.driver.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    @Resource
    private TencentCloudProperties tencentCloudProperties;

    /**
     * 腾讯云COS客户端
     * @return cosClient
     */
    private COSClient getPrivateCOSClient() {
        // 初始化用户身份信息
        COSCredentials cred = new BasicCOSCredentials(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());
        // 设置bucket的地域
        ClientConfig clientConfig = new ClientConfig(new Region(tencentCloudProperties.getRegion()));
        // 设置HttpProtocol
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 生成cos客户端
        COSClient cosClient = new COSClient(cred, clientConfig);
        // 返回cos客户端
        return cosClient;
    }

    /**
     * 文件上传
     * @param file 要上传的文件
     * @param path 文件存储的路径
     * @return CosUploadVo
     * 参考文档：https://console.cloud.tencent.com/cos
     *         https://cloud.tencent.com/document/product/436/10199
     */
    @SneakyThrows
    @Override
    public CosUploadVo upload(MultipartFile file, String path) {
        // cos客户端
        COSClient cosClient = this.getPrivateCOSClient();

        // 设置元数据信息
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(file.getSize());
        meta.setContentEncoding("UTF-8");
        meta.setContentType(file.getContentType());

        // 向存储桶中保存文件
        String fileType = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")); // 文件后缀名
        // 文件路径、唯一文件名
        String uploadPath = "/driver/" + path + "/" + UUID.randomUUID().toString().replaceAll("-", "") + fileType;
        // 设置相关属性
        PutObjectRequest putObjectRequest = new PutObjectRequest(tencentCloudProperties.getBucketPrivate(), uploadPath, file.getInputStream(), meta);
        putObjectRequest.setStorageClass(StorageClass.Standard);  // 设置存储方式
        // 上传文件
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);

        log.info(JSON.toJSONString(putObjectResult));

        // 上传完关闭cos客户端
        cosClient.shutdown();

        // 封装返回对象
        CosUploadVo cosUploadVo = new CosUploadVo();
        cosUploadVo.setUrl(uploadPath);
        // 生成图片临时访问url，回显使用
        cosUploadVo.setShowUrl(this.getImageUrl(uploadPath));

        return cosUploadVo;
    }

    /**
     * 获取带临时签名的图片地址
     * @param path 图片地址
     * @return 带有临时签名的图片URL
     * 参考文档：https://cloud.tencent.com/document/product/436/35217
     */
    @Override
    public String getImageUrl(String path) {
        if(!StringUtils.hasText(path)) return "";
        // cos客户端
        COSClient cosClient = this.getPrivateCOSClient();
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(tencentCloudProperties.getBucketPrivate(), path, HttpMethodName.GET);
        // 设置临时URL有效期为15分钟
        Date expiration = new DateTime().plusMinutes(15).toDate();
        request.setExpiration(expiration);
        URL url = cosClient.generatePresignedUrl(request);
        // 关闭cos客户端
        cosClient.shutdown();
        // 返回临时地址
        return url.toString();
    }
}
