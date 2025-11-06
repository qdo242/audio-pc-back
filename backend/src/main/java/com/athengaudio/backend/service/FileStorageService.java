package com.athengaudio.backend.service;

import java.io.IOException;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;

@Service
public class FileStorageService {

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFsOperations operations;

    /**
     * Lưu file vào MongoDB GridFS
     * 
     * @param file File được tải lên
     * @return ID của file đã lưu (dưới dạng String)
     * @throws IOException
     */
    public String store(MultipartFile file) throws IOException {
        // Tạo metadata cho file
        DBObject metaData = new BasicDBObject();
        metaData.put("fileName", file.getOriginalFilename());
        metaData.put("contentType", file.getContentType());
        metaData.put("size", file.getSize());

        // Lưu file vào GridFS và trả về ID
        ObjectId id = gridFsTemplate.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                metaData);
        return id.toString();
    }

    /**
     * Lấy file từ GridFS bằng ID
     * 
     * @param id ID của file (dưới dạng String)
     * @return GridFSFile object
     * @throws IllegalStateException
     */
    public GridFSFile getFile(String id) throws IllegalStateException {
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(id)));
        if (file == null) {
            throw new IllegalStateException("Không tìm thấy file với ID: " + id);
        }
        return file;
    }

    /**
     * Lấy resource (nội dung file) để streaming
     * 
     * @param file GridFSFile
     * @return GridFsResource
     */
    public org.springframework.data.mongodb.gridfs.GridFsResource getFileAsResource(GridFSFile file) {
        return operations.getResource(file);
    }
}