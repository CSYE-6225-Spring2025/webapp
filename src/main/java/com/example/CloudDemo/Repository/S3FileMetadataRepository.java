package com.example.CloudDemo.Repository;

import com.example.CloudDemo.Model.S3MetaData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface S3FileMetadataRepository extends JpaRepository<S3MetaData, Long> {
    Optional<S3MetaData> findByFileName(String fileName);
    void deleteByFileID(String fileID);
}
