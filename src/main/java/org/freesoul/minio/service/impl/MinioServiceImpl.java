package org.freesoul.minio.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.freesoul.minio.service.MinioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;

/**
 * @author 吴智兴
 * @since 2024/05/30
 */
@Service
public class MinioServiceImpl implements MinioService {

    Logger logger = LoggerFactory.getLogger(MinioServiceImpl.class);

    @Value("${minio.bucket}")
    private String bucket;
    @Resource
    private MinioClient minioClient;

    /**
     * 校验文件是否存在
     *
     * @param md5        文件md5值
     * @param fileSuffix 文件后缀
     * @return 是否存在
     */
    @Override
    public Boolean checkExits(String md5, String fileSuffix) {
        // 可以将 md5 存入 redis 等，不用每次都访问 minio
        GetObjectResponse objectResponse;
        try {
            objectResponse = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object("files/" + md5 + fileSuffix).build());
        } catch (Exception e) {
            logger.warn("getObject error", e);
            return false;
        }
        return objectResponse != null;
    }

    /**
     * 上传文件分片
     *
     * @param md5         文件md5值
     * @param chunkIndex  分块索引
     * @param inputStream 分块数据流
     * @param fileSize    分块文件大小
     * @return 上传结果
     */
    @Override
    public Boolean upload(String md5, Integer chunkIndex, InputStream inputStream, long fileSize) {
        logger.info("开始上传分片");
        GetObjectResponse objectResponse = getObject("chunks/" + md5 + "/" + chunkIndex);
        if (objectResponse != null) {
            try {
                objectResponse.close();
            } catch (IOException e) {
                logger.error("close error", e);
                throw new RuntimeException(e);
            }
            return true;
        }
        try {
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucket).object("chunks/" + md5 + "/" + chunkIndex).stream(inputStream, fileSize, -1).build());
        } catch (Exception e) {
            logger.error("upload error", e);
            throw new RuntimeException(String.format("minio 存储对象失败，【%s】", e.getMessage()));
        }
        logger.info("上传分片结束");
        return true;
    }

    /**
     * 合并文件
     *
     * @param md5        文件md5值
     * @param chunkTotal 文件分片总数
     * @param fileSuffix 文件后缀
     * @return 合并结果
     */
    @Override
    public String merge(String md5, Integer chunkTotal, String fileSuffix) {
        logger.info("开始合并");

        // 获取所以分块
        List<Item> itemList = getChunkList(md5);
        // 获取缺失的分块
        List<Integer> missChunkIndexList = getMissChunkIndexList(itemList, chunkTotal);
        if (!missChunkIndexList.isEmpty()) {
            logger.warn("miss chunk index, chunkIndexList: {}", missChunkIndexList);
            return String.format("[miss_chunk]%s", missChunkIndexList);
        }

        // 合并文件
        List<ComposeSource> composeSourceList = new ArrayList<>();
        for (Item item : itemList) {
            composeSourceList.add(ComposeSource.builder().bucket(bucket).object(item.objectName()).build());
        }
        try {
            logger.info("正在合并");
            minioClient.composeObject(
                    ComposeObjectArgs.builder().bucket(bucket).object("files/" + md5 + fileSuffix).sources(composeSourceList).build());
        } catch (Exception e) {
            logger.error("merge error", e);
            throw new RuntimeException(String.format("minio 合并对象失败，【%s】", e.getMessage()));
        }

        // 获取文件url
        try {
            logger.info("合并结束");
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder().method(Method.GET).bucket(bucket).object("files/" + md5 + fileSuffix).expiry(1, TimeUnit.DAYS)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取所有分块
     *
     * @param md5 文件md5值
     * @return 分块列表
     */
    private List<Item> getChunkList(String md5) {
        // 获取所有分片
        Iterable<Result<Item>> resultIterable =
                minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).prefix("chunks/" + md5 + "/").recursive(false) // 是否递归查询
                        .build());

        List<Item> itemList = new ArrayList<>(); // 分块

        for (Result<Item> itemResult : resultIterable) {
            try {
                itemList.add(itemResult.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        // 分片文件排序
        itemList.sort((o1, o2) -> {
            String o1Name = o1.objectName();
            String o2Name = o2.objectName();
            int o1Index = Integer.parseInt(o1Name.substring(o1Name.lastIndexOf("/") + 1));
            int o2Index = Integer.parseInt(o2Name.substring(o2Name.lastIndexOf("/") + 1));
            return o1Index - o2Index;
        });
        return itemList;
    }

    /**
     * 获取缺失的分片
     *
     * @param chunkList  分片列表
     * @param chunkTotal 分片总数
     * @return 缺失的分片列表
     */
    private List<Integer> getMissChunkIndexList(List<Item> chunkList, Integer chunkTotal) {
        List<Integer> missChunkIndexList = new ArrayList<>(chunkTotal); // 缺失的分片文件
        int index = 1;
        for (Item item : chunkList) {
            String chunkName = item.objectName();
            int chunkIndex = Integer.parseInt(chunkName.substring(chunkName.lastIndexOf("/") + 1));
            if (index != chunkIndex) {
                missChunkIndexList.add(chunkIndex);
            }
            index++;
        }
        if (chunkTotal > index) {
            for (int i = index; i <= chunkTotal; i++) {
                missChunkIndexList.add(i);
            }
        }
        return missChunkIndexList;
    }

    /**
     * 获取文件
     *
     * @param object 文件名
     * @return 文件流
     */
    private GetObjectResponse getObject(String object) {
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(object).build());
        } catch (Exception e) {
            logger.error("getObject error", e);
        }
        return null;
    }
}
