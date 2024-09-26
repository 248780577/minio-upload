package org.freesoul.minio.service;

import java.io.InputStream;

/**
 * @author 吴智兴
 * @since 2024/05/30
 */
public interface MinioService {
    Boolean checkExits(String md5, String fileSuffix);

    Boolean upload(String md5,  Integer chunkIndex, InputStream inputStream, long fileSize);

    String merge(String md5, Integer chunkTotal,String fileSuffix);
}
