package com.example.admin.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
@CrossOrigin
public class FileController {

    /** 与 WebMvcConfig 静态资源一致：JVM 工作目录下的 uploads（绝对路径，避免 Windows 上 transferTo 失败） */
    private Path getUploadDir() throws IOException {
        Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        return uploadPath;
    }

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }

        try {
            Path uploadPath = getUploadDir();

            String originalName = file.getOriginalFilename();
            if (originalName == null) {
                originalName = "unknown";
            }

            String ext = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0) {
                ext = originalName.substring(dot).toLowerCase();
            }
            if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png")) {
                return Result.error("仅支持 JPG、JPEG、PNG 图片");
            }
            // 拖拽文件时浏览器常带 application/octet-stream，仍以扩展名为准
            String ct = file.getContentType();
            if (ct != null && !ct.isBlank()
                    && !ct.startsWith("image/")
                    && !"application/octet-stream".equalsIgnoreCase(ct.trim())) {
                return Result.error("仅支持图片文件");
            }

            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = uploadPath.resolve(filename).normalize();
            if (!target.startsWith(uploadPath)) {
                return Result.error("非法文件路径");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // 前端通过该 URL 访问文件
            String url = "/uploads/" + filename;

            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            data.put("fileName", originalName);

            return Result.success(data);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败");
        }
    }
}

