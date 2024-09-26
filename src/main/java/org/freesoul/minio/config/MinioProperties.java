package org.freesoul.minio.config;

import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 吴智兴
 * @since 2024/05/30
 */
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucketName;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MinioProperties that = (MinioProperties) o;
        return Objects.equals(endpoint, that.endpoint) && Objects.equals(accessKey, that.accessKey) &&
                Objects.equals(secretKey, that.secretKey) && Objects.equals(bucketName, that.bucketName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, accessKey, secretKey, bucketName);
    }

    @Override
    public String toString() {
        return "MinioProperties{" +
                "endpoint='" + endpoint + '\'' +
                ", accessKey='" + accessKey + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", bucketName='" + bucketName + '\'' +
                '}';
    }
}
