package cn.edu.xidian.cephos.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

/**
 * 对象存储工具类
 * s3客户端类使用了连接池所有相关对象都使用单例component
 */
@Slf4j
@Component
public class ObjectStorage {
    @Resource(name = "cephConnection")
    private  AmazonS3 cephConnection;
    /**
     * 有效期时长,单位默认为日,最长7日
     */
    private  final int signedTime = 7;

    /**
     * 上传对象至ceph
     * @param bucketName 桶名
     * @param objName 对象名
     * @param file 数据流
     * @return Etag
     * @throws SdkClientException 上传ceph中发生错误
     */
    public  String putObject(String bucketName, String objName, File file) throws SdkClientException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.length());
        PutObjectResult result;
        try {
            result = cephConnection.putObject(bucketName, objName, file);
        }catch (SdkClientException e){
            log.error("传输异常", e);
            e.printStackTrace();
            throw e;
        }
        return result.getContentMd5();
    }

    /**
     * 生成对象7日有效期的url
     * @param bucketName 桶名
     * @param objName 文件名
     * @param method http请求方法
     * @return url
     */
    public  String getObjPath(String bucketName, String objName, HttpMethod method){
        Calendar c=Calendar.getInstance();
        c.add(Calendar.DATE, signedTime);
        return cephConnection.generatePresignedUrl(bucketName, objName, c.getTime(), method).toString();
    }

    /**
     * 生成对象7日有效期的put请求url
     * @param bucketName 桶名
     * @param objName 文件名
     * @return url
     */
    public  String objPutPath(String bucketName, String objName){
        return getObjPath(bucketName, objName, HttpMethod.PUT);
    }

    /**
     * 生成对象7日有效期的get请求url
     * @param bucketName 桶名
     * @param objName 文件名
     * @return url
     */
    public  String objGetPath(String bucketName, String objName){
        return getObjPath(bucketName, objName, HttpMethod.GET);
    }

    /**
     * 获取桶列表
     * @return 桶列表
     */
    public  List<Bucket> getBucketList(){
        return cephConnection.listBuckets();
    }

    /**
     * 创建桶
     * @param bucketName 桶名
     * @return true 成功 false 失败
     */
    public  boolean createBucket(String bucketName){
        if(cephConnection.doesBucketExistV2(bucketName)){
            log.error("桶已存在: {}", bucketName);
            return false;
        }
        try{
            cephConnection.createBucket(bucketName);
        }catch (AmazonServiceException e){
            log.error("创建桶: {} 出错: {}", bucketName, e.getErrorMessage());
            e.printStackTrace();
            return false;
        }
        log.info("创建桶: {} 成功", bucketName);
        return true;
    }

    /**
     * 获取指定桶
     * @param bucketName 桶名
     * @return 桶
     */
    public  Bucket getBucket(String bucketName){
        Optional<Bucket> optional = cephConnection.listBuckets().stream().filter((bucket) -> bucket.getName().equals(bucketName))
                .findFirst();
        if(!optional.isPresent()){
            log.error("桶不存在: {}", bucketName);
            return null;
        }
        return optional.get();
    }

    /**
     * 清空桶中已有所有对象和所有版本并删除桶
     * @param bucketName 桶名
     * @return true 成功 false 失败
     */
    public  boolean deleteBucket(String bucketName){
        try {
            ObjectListing object_listing = cephConnection.listObjects(bucketName);
            log.warn("清空桶: {} 中所有对象", bucketName);
            while (true) {
                for (S3ObjectSummary summary : object_listing.getObjectSummaries()) {
                    cephConnection.deleteObject(bucketName, summary.getKey());
                }
                if (object_listing.isTruncated()) {
                    object_listing = cephConnection.listNextBatchOfObjects(object_listing);
                } else {
                    break;
                }
            }
            log.warn("清空桶: {} 中所有多版本控制对象", bucketName);
            VersionListing version_listing = cephConnection.listVersions(new ListVersionsRequest().withBucketName(bucketName));
            while (true) {
                for (S3VersionSummary vs : version_listing.getVersionSummaries()) {
                    cephConnection.deleteVersion(
                            bucketName, vs.getKey(), vs.getVersionId());
                }

                if (version_listing.isTruncated()) {
                    version_listing = cephConnection.listNextBatchOfVersions(version_listing);
                } else {
                    break;
                }
            }
            log.warn("删除桶: {}", bucketName);
            cephConnection.deleteBucket(bucketName);
        } catch (AmazonServiceException e) {
            log.error("删除桶: {} 出错: {}", bucketName, e.getErrorMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 获取对象从其元数据中的Etag取得MD5
     * @param bucketName 桶名
     * @param objName 对象名
     * @return MD5字符串
     */
    public  String getObjectMD5(String bucketName, String objName){
        S3Object object= cephConnection.getObject(bucketName, objName);
        if(object==null){
            log.error("对象不存在: {}", objName);
            return null;
        }
        return object.getObjectMetadata().getETag();
    }

    /**
     * 删除对象
     * @param bucketName 桶名
     * @param objName 对象名
     * @return true 成功 false 失败
     */
    public  boolean deleteObject(String bucketName, String objName){
        try {
            cephConnection.deleteObject(bucketName, objName);
        } catch (AmazonServiceException e) {
            log.error("删除对象: {} 出错: {}", objName, e.getErrorMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
