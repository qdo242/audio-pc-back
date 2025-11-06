package com.athengaudio.backend.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.athengaudio.backend.service.FileStorageService;
import com.mongodb.client.gridfs.model.GridFSFile;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:4200") // Cho phép frontend gọi
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Endpoint để upload file
     * 
     * @param file Dữ liệu file từ form-data
     * @return URL để truy cập file
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileId = fileStorageService.store(file);
            // Trả về URL tương đối để frontend sử dụng
            String fileUrl = "/api/files/" + fileId;
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "File uploaded successfully",
                    "url", fileUrl // Frontend sẽ lưu URL này
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * Endpoint để xem/tải file (dùng cho thẻ <img>)
     * 
     * @param id ID của file trong GridFS
     * @return Dữ liệu ảnh
     */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getFile(@PathVariable String id) {
        try {
            GridFSFile file = fileStorageService.getFile(id);
            GridFsResource resource = fileStorageService.getFileAsResource(file);

            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf(file.getMetadata().get("contentType").toString()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                    .body(resource);
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        }
    }
}