package com.example.CloudDemo.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "s3_file_metadata")
public class S3MetaData {
    @Id
    private String fileID;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private long fileSize;
    private LocalDateTime uploadTime;

    public S3MetaData() {
    }

    public S3MetaData(String fileID, String fileName, String fileUrl, String fileType, long fileSize, LocalDateTime uploadTime) {
        this.fileID=fileID;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadTime = uploadTime;
    }
}
