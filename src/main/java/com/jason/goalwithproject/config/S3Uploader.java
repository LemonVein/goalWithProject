package com.jason.goalwithproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile file, String dirName) throws IOException {
        String originalName = file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.]", "_");

        String fileName = dirName + "/" + UUID.randomUUID() + "_" + originalName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

        return getFileUrl(fileName);
    }

    public void deleteFile(String fileUrl) {
        int idx = fileUrl.indexOf(".amazonaws.com/");
        if (idx == -1) {
            throw new IllegalArgumentException("잘못된 S3 URL 형식입니다: " + fileUrl);
        }

        String key = fileUrl.substring(idx + ".amazonaws.com/".length());

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    private String getFileUrl(String fileName) {
        return "https://" + bucket + ".s3.amazonaws.com/" + fileName;
    }
}
