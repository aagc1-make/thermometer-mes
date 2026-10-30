package com.ktg.common.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minio 配置信息
 *
 */
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    /**
     * 服务地址
     */
    private static String url;

    /**
     * 用户名
     */
    private static String accessKey;

    /**
     * 密码
     */
    private static String secretKey;

    /**
     * 存储桶名称
     */
    private static String bucketName;

    public static String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        MinioConfig.url = url;
    }

    public static String getAccessKey()
    {
        return accessKey;
    }

    public void setAccessKey(String accessKey)
    {
        MinioConfig.accessKey = accessKey;
    }

    public static String getSecretKey()
    {
        return secretKey;
    }

    public void setSecretKey(String secretKey)
    {
        MinioConfig.secretKey = secretKey;
    }

    public static String getBucketName()
    {
        return bucketName;
    }

    public void setBucketName(String bucketName)
    {
        MinioConfig.bucketName = bucketName;
    }

    @Bean
    public MinioClient getMinioClient()
    {
        return MinioClient.builder().endpoint(url).credentials(accessKey, secretKey).build();
    }

    /**
     * 启动时确保存储桶存在，并允许匿名读取对象。
     * 演示项目使用公开只读桶；上传仍由后端鉴权完成，不开放匿名写入。
     * MinIO 暂时不可用时只记录警告，不阻断 MES 启动。
     */
    @Bean
    public ApplicationRunner minioBucketInitializer(MinioClient minioClient)
    {
        return args -> {
            try
            {
                if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()))
                {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                }
                String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\","
                        + "\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],"
                        + "\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}]}";
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucketName).config(policy).build());
                log.info("MinIO bucket [{}] ready, public read enabled", bucketName);
            }
            catch (Exception e)
            {
                log.warn("MinIO bucket [{}] init failed: {}", bucketName, e.getMessage());
            }
        };
    }
}
