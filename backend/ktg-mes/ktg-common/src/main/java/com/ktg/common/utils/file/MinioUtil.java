package com.ktg.common.utils.file;

import com.ktg.common.config.MinioConfig;
import com.ktg.common.utils.spring.SpringUtils;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public class MinioUtil {
    /**
     * 上传文件
     *
     * @param bucketName 桶名称
     * @param fileName
     * @throws IOException
     */
    public static String uploadFile(String bucketName, String fileName, MultipartFile multipartFile) throws IOException
    {
        MinioClient minioClient = SpringUtils.getBean(MinioClient.class);
        try (InputStream inputStream = multipartFile.getInputStream())
        {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(inputStream, multipartFile.getSize(), -1).contentType(multipartFile.getContentType()).build());
            // 保存永久对象地址，而不是把短期预签名 URL 存进业务表。
            // 桶由 MinioConfig 启动时设置为公开只读，因此该地址在服务重启后仍可访问。
            String base = MinioConfig.getUrl();
            if (base.endsWith("/"))
            {
                base = base.substring(0, base.length() - 1);
            }
            return base + "/" + bucketName + "/" + fileName;
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }
}
