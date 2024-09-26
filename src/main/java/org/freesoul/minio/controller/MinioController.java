package org.freesoul.minio.controller;

import java.io.IOException;

import org.freesoul.minio.service.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * <b> 分片上传 + 端点续传 + 秒传 </b>
 *
 * <p> 一、前端直接上传到minio
 * <ul>
 *  <li> 1、前端上传之前，用md5值查询文件是否存在，存在直接返回文件访问地址，实现秒传
 *  <li> 2、前端向获取上传凭证，后端返回上传凭证
 *  <li> 3、前端对文件切片，上传之前询问后端，'/chunks/文件md5值/分块索引'路径下文件是否存在？
 *      <ul>
 *          <li> 存在，跳过，上传下一个分片
 *          <li> 不存在，上传到minio，路径为'/chunks/文件md5值/分块索引'
 *      </ul>
 *  <li> 4、前端全部发送完毕，发送合并文件请求并带上文件md5值和总分片数，后端根据文件md5值查询分片，并对比数量是否都存在。
 *      <ul>
 *          <li> 存在，合并文件，返回文件访问地址
 *          <li> 不存在，返回错误信息，前端重新上传缺少的那块分片
 *      </ul>
 * </ul>
 *
 * <p> 二、前端先上传到服务器，服务器再上传到minio
 * <ul>
 * <li> 1、前端上传之前，用md5值查询文件是否存在，存在直接返回文件访问地址，实现秒传
 * <li> 2、前端对文件分片，上传到服务器，参数除了文件本身，还需要文件的md5值和分片文件的索引
 * <li> 3、服务器判断/chunks/文件md5值/分块索引'是否存在，存在直接返回成功，否则上传到minio到'/chunks/文件md5值/分块索引'路径
 * <li> 4、前端发送完毕请求，参数为文件的md5值和总分片数，后端根据文件md5值查询分片，并对比数量是否都存在。
 *      <ul>
 *          <li> 存在，合并文件，返回文件访问地址
 *          <li> 不存在，返回错误信息，前端重新上传缺少的那块分片
 *      </ul>
 * </ul>
 * <p> 端点续传在哪里？
 * <p> 我们可以让分片目录存活一段时间。用户上传3个分片后网络异常，重新连接网络后，已经上传的3个分片已经存在，无需从新上传
 * <p> 还有一种方式：后端实现分片上传，前端实现秒传。但是这样明显不合适，用户仍然需要等待文件上传到后端，即技术上实现分片，用户体验上没有实现。
 * <p> 我们最后向minio中存储的文件名称都是md5+文件后缀，对于文件管理并不友好，推荐维护一张表，存储上传日期、原始文件名称等信息
 *
 * @author 吴智兴
 * @since 2024/05/29
 */
@RestController
@RequestMapping("/minio")
public class MinioController {

    @Autowired
    private MinioService minioService;


    @GetMapping("/checkExits")
    public ResponseEntity<Boolean> checkExits(@RequestParam String md5, @RequestParam String fileSuffix) {
        return ResponseEntity.ok(minioService.checkExits(md5, fileSuffix));
    }

    @PostMapping("/upload")
    public ResponseEntity<Boolean> upload(@RequestParam String md5, @RequestParam Integer chunkIndex, @RequestParam MultipartFile chunk)
            throws IOException {
        return ResponseEntity.ok(minioService.upload(md5, chunkIndex, chunk.getInputStream(), chunk.getSize()));
    }

    @PostMapping("/merge")
    public ResponseEntity<String> merge(@RequestParam String md5, @RequestParam Integer chunkTotal, @RequestParam String fileSuffix) {
        return ResponseEntity.ok(minioService.merge(md5, chunkTotal, fileSuffix));
    }
}
