package com.study.event.api.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileUploadService {

    private final AwsS3Service s3Service;

    /**
     * 파일 업로드 처리
     * @param profileImage - 클라이언트가 전송한 파일 바이너리 객체
     * @return - 업로드된 파일의 URL
     */
    public String uploadProfileImage(MultipartFile profileImage) throws IOException {

        // 파일명을 유니크하게 변경
        String uniqueFileName = UUID.randomUUID() + "_" + profileImage.getOriginalFilename();

        // 파일을 S3 버킷에 저장 (핵심)
        // (AMAZON에 연결할 수 있는 api를 gradle에 추가하고, yml에서 코드 추가)
        String url = s3Service.uploadToS3Bucket(profileImage.getBytes(), uniqueFileName);

        return url;
    }
}
