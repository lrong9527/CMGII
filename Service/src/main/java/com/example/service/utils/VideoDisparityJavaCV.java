package com.example.service.utils;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avutil.Callback_Pointer_int_BytePointer_Pointer;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_java;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;

@Slf4j
@Component
public class VideoDisparityJavaCV {
    public String leftToright(String uploadDirectory,String leftVideoName) {
        // 加载 OpenCV 库
        Loader.load(opencv_java.class);
        String leftVideoPath = uploadDirectory + File.separator + leftVideoName;

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(leftVideoPath)) {
            grabber.start();

            int videoWidth = grabber.getImageWidth();
            int videoHeight = grabber.getImageHeight();

            String rightVideoName = "R" + leftVideoName;
            String rightVideoPath = uploadDirectory + File.separator + rightVideoName;

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(rightVideoPath, videoWidth, videoHeight)) {
                recorder.setVideoCodec(grabber.getVideoCodec());
                recorder.setFrameRate(grabber.getFrameRate());
                recorder.setVideoQuality(0);
                //recorder.setPixelFormat(grabber.getPixelFormat());
                recorder.start();

                int disparityValue = 5; // 设置视差值（向右平移的像素数）

                Java2DFrameConverter converter = new Java2DFrameConverter();
                Frame frame;

                while ((frame = grabber.grab()) != null) {
                    BufferedImage bufferedImage = converter.convert(frame);
                    BufferedImage shiftedImage = new BufferedImage(videoWidth, videoHeight, BufferedImage.TYPE_3BYTE_BGR);

                    for (int y = 0; y < videoHeight; y++) {
                        for (int x = 0; x < videoWidth; x++) {
                            int newX = x + disparityValue;
                            if (newX >= 0 && newX < videoWidth) {
                                int rgb = 0;
                                if (bufferedImage != null) {
                                    rgb = bufferedImage.getRGB(x, y);
                                }
                                shiftedImage.setRGB(newX, y, rgb);
                            }
                        }
                    }

                    Frame shiftedFrame = converter.convert(shiftedImage);
                    recorder.record(shiftedFrame);
                }
                recorder.stop();
            }

            log.info("视差平移完成，新视频已保存。");
            return rightVideoName;
        } catch (Exception e) {
            log.error(String.valueOf(e));
            return "error";
        }
    }
}
