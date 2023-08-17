package com.example.service.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
@Component
public class TransCoder {
    public String _2DTo_3D(String uploadDirectory, String leftVideoName, String rightVideoName){
        int index = leftVideoName.indexOf(".avi");
        String filename = "3D_"+leftVideoName.substring(0, index)+".mp4";

        String cmd2 ="ffmpeg -i "+uploadDirectory + File.separator +leftVideoName+" -i "
                +uploadDirectory + File.separator +rightVideoName
                + " -filter_complex \"[0:v]pad=iw*2:ih[int];[int][1:v]overlay=W/2:0[vid]\" -map \"[vid]\" -c:v libx264 -crf 23 -preset veryfast -c:a copy " + uploadDirectory + File.separator +filename;
        try {
            // 执行FFmpeg命令
            Process process = Runtime.getRuntime().exec(cmd2);

            // 异步处理进程的标准输出流
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 处理输出流的内容
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    log.error("ffmpeg处理输出流出现异常：" + e.getMessage());
                }
            }).start();

            // 异步处理进程的标准错误流
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 处理错误流的内容
                        System.err.println(line);
                    }
                } catch (IOException e) {
                    log.error("ffmpeg处理错误流出现异常：" + e.getMessage());
                }
            }).start();


            // 等待命令执行完成
            int exitCode = process.waitFor();
            // 在等待完成之后销毁进程
            process.destroy();
            if(exitCode==0){
                log.info("2D转3D转码完成，退出码：" + exitCode);
                return filename;
            }
            else {
                log.info("2D转3D转码失败，退出码：" + exitCode);
                return "error";
            }
        } catch (IOException | InterruptedException e) {
            log.error("2D转3D转码过程中出现异常：" + e.getMessage());
        }
        return "error";
    }
}
