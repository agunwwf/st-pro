package com.example.admin.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
@CrossOrigin
public class FileController {

    // 简单起见，直接使用项目根目录下的 uploads 目录
    private static final String UPLOAD_DIR = "uploads";

    private Path getUploadDir() throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
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
                ext = originalName.substring(dot);
            }

            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = uploadPath.resolve(filename);
            file.transferTo(target.toFile());

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

