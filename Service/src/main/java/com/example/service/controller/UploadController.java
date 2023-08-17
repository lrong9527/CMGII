package com.example.service.controller;

import cn.edu.xidian.cephos.service.ObjectStorage;
import com.example.service.utils.TransCoder;
import com.example.service.utils.VideoDisparityJavaCV;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

@RestController
@Slf4j
public class UploadController {
    @Value("${file.upload.directory}") // 配置文件中设置保存文件的目录路径
    private String uploadDirectory;
    @Resource
    ObjectStorage objectStorage;
    @Resource
    TransCoder transCoder;
    @Resource
    VideoDisparityJavaCV videoDisparityJavaCV;
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>("Please select a file to upload.", HttpStatus.BAD_REQUEST);
        }
        String fileName = file.getOriginalFilename();
        String filePath = uploadDirectory + File.separator + fileName;
        try {
            File _2DFile=new File(filePath);
            file.transferTo(_2DFile);

            String rightVideoName= videoDisparityJavaCV.leftToright(uploadDirectory,fileName);
            if(rightVideoName.equals("error"))return new ResponseEntity<>("视差平移操作失败.", HttpStatus.INTERNAL_SERVER_ERROR);
            File rightVideo=new File(uploadDirectory + File.separator+rightVideoName);
            String _3DfileName=transCoder._2DTo_3D(uploadDirectory,fileName,rightVideoName);
            if(_3DfileName.equals("error"))return new ResponseEntity<>("2D转3D转码失败失败.", HttpStatus.INTERNAL_SERVER_ERROR);
            File _3DFile=new File(uploadDirectory + File.separator+_3DfileName);
            objectStorage.putObject("test",_3DfileName,_3DFile);
            String url=objectStorage.objGetPath("test",_3DfileName);
            log.info("文件下载链接已生成："+url);
            return new ResponseEntity<>(url, HttpStatus.OK);

        } catch (Exception e) {
            log.error(String.valueOf(e));
            return new ResponseEntity<>("File upload failed.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

