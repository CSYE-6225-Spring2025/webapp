package com.example.CloudDemo.Service;


import com.example.CloudDemo.DTO.UploadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AWSs3service {

    @Autowired
    private S3Client s3Client;

    @Value("${aws.bucket.name}")
    private String bucketName;

    public List<Bucket> getBucketList() {
        ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
        return listBucketsResponse.buckets();
    }

    public UploadResponse uploadFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String fileId = UUID.randomUUID().toString();
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        String objectKey = fileId;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));


        return new UploadResponse(fileName, fileId, bucketName + "/" + fileId + "/" + fileName, LocalDate.now().toString());
    }



    public UploadResponse getFileById(String fileId) {
        try {
            HeadObjectRequest metadataRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .build();
            var metadataResponse = s3Client.headObject(metadataRequest);
            String uploadDate = metadataResponse.lastModified().atZone(java.time.ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = metadataResponse.metadata().get("original-filename");
            if (fileName == null) fileName = fileId;
            String formattedUrl = String.format("%s/%s/%s", bucketName, fileId,fileName);
            return new UploadResponse(fileName, fileId, formattedUrl, uploadDate);
        } catch (Exception e) {
            throw new RuntimeException("File not found for ID: " + fileId);
        }
    }

    public void deleteFileById(String fileId) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (NoSuchKeyException e) {
            throw new RuntimeException("File not found for ID: " + fileId);
        }
    }
}
