package com.knowledge.common.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.knowledge.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

/**
 * 阿里云 OSS 对象存储:头像等图片上传
 */
@Slf4j
@Service
public class OssStorageService {

    /**
     * 允许上传的图片类型白名单
     */
    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    /**
     * 头像大小上限 2MB
     */
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024L;

    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.access-key-id}")
    private String accessKeyId;

    @Value("${oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${oss.bucket-name}")
    private String bucketName;

    @Value("${oss.url-prefix}")
    private String urlPrefix;

    /**
     * 上传图片到 OSS
     * @param file 图片文件
     * @param dir  对象存储目录,如 avatar
     * @param ownerId 归属者 id(放进路径,便于追溯与管理)
     * @return 可访问的完整 URL
     */
    public String uploadImage(MultipartFile file, String dir, Long ownerId) {
        // 1.配置校验:没配 AK 时明确报错,而不是含糊的 OSS 异常
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(accessKeyId)
                || !StringUtils.hasText(bucketName) || !StringUtils.hasText(urlPrefix)) {
            throw new BusinessException(500, "OSS 未配置，请联系管理员");
        }

        // 2.图片类型白名单(小写匹配防绕过)
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalName.substring(dot + 1).toLowerCase();
        }
        if (!ALLOWED_IMAGE_EXT.contains(ext)) {
            throw new BusinessException(400, "仅支持 jpg/png/gif/webp 图片");
        }

        // 3.大小限制 2MB
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(400, "图片不能超过 2MB");
        }

        // 4.对象 key:dir/{ownerId}/{uuid}.{ext};UUID 防重名与路径穿越
        String objectKey = dir + "/" + ownerId + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        // 5.上传(OSSClient 非线程安全,每次构建用完关闭;头像上传低频,成本可忽略)
        OSS client = null;
        try (InputStream in = file.getInputStream()) {
            client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            client.putObject(bucketName, objectKey, in);
        } catch (IOException e) {
            log.error("OSS 上传失败, objectKey={}", objectKey, e);
            throw new BusinessException(500, "图片上传失败");
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
        return urlPrefix + "/" + objectKey;
    }
}
