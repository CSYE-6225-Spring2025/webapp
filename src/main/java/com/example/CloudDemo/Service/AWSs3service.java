package com.example.CloudDemo.Service;


import com.example.CloudDemo.DTO.UploadResponse;
import com.example.CloudDemo.Model.S3MetaData;
import com.example.CloudDemo.Repository.S3FileMetadataRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AWSs3service {

    @Autowired
    private S3Client s3Client;

    @Value("${aws.bucket.name}")
    private String bucketName;

    @Autowired
    S3FileMetadataRepository s3FileMetadataRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    private static final Logger logger = LoggerFactory.getLogger(AWSs3service.class);


    public List<Bucket> getBucketList() {
        ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
        return listBucketsResponse.buckets();
    }

    public UploadResponse uploadFile(MultipartFile file) throws IOException {
        Timer.Sample s3Sample = Timer.start(meterRegistry);
        String fileName = file.getOriginalFilename();
        String fileId = UUID.randomUUID().toString();
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        String objectKey = fileId;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .metadata(Map.of("filename", file.getOriginalFilename()))
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
        S3MetaData metadata = new S3MetaData(
                fileId,
                file.getOriginalFilename(),
                bucketName + "/" + fileId + "/" + fileName,
                file.getContentType(),
                file.getSize(),
                LocalDateTime.now()
        );
        Timer.Sample dbSample = Timer.start(meterRegistry);
        s3FileMetadataRepository.save(metadata);
        dbSample.stop(meterRegistry.timer("db.duration", "operation", "saveS3FileMetadata"));
        s3Sample.stop(meterRegistry.timer("s3.duration", "operation", "postObject"));
        return new UploadResponse(fileName, fileId, bucketName + "/" + fileId + "/" + fileName, LocalDate.now().toString());
    }


    public UploadResponse getFileById(String fileId) {
        Timer.Sample s3Sample = null;
        try {
            s3Sample = Timer.start(meterRegistry);
            HeadObjectRequest metadataRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .build();
            var metadataResponse = s3Client.headObject(metadataRequest);
            String uploadDate = metadataResponse.lastModified().atZone(java.time.ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = metadataResponse.metadata().get("filename");
            if (fileName == null) fileName = fileId;
            return new UploadResponse(fileName, fileId, bucketName + "/" + fileId + "/" + fileName, uploadDate);
        } catch (Exception e) {
            logger.error("S3Exception in getFileById for fileId: {}", fileId, e);
            throw new RuntimeException("File not found for ID: " + fileId);
        } finally {
            assert s3Sample != null;
            s3Sample.stop(meterRegistry.timer("s3.duration", "operation", "headObject"));
        }
    }

    @Transactional
    public void deleteFileById(String fileId) {
        Timer.Sample s3Sample = Timer.start(meterRegistry);
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .build());
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .build();

            s3Client.deleteObject(deleteRequest);
            s3Sample.stop(meterRegistry.timer("s3.duration", "operation", "deleteObject"));
            Timer.Sample dbSample = Timer.start(meterRegistry);
            s3FileMetadataRepository.deleteByFileID(fileId);
            dbSample.stop(meterRegistry.timer("db.duration", "operation", "deleteS3FileMetadata"));
        } catch (S3Exception e) {
            logger.error("S3Exception in deleteFileById for fileId: {}", fileId, e);
            throw new RuntimeException("File not found for ID: " + fileId);
        }
    }
}
