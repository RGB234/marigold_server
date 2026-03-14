package com.sns.marigold.storage.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import com.sns.marigold.global.dto.ApiResponse;
import com.sns.marigold.storage.service.S3Service;
import com.sns.marigold.global.UrlConstants;

@RestController
@RequestMapping(UrlConstants.STORAGE_BASE)
@RequiredArgsConstructor
public class StorageController {

    private final S3Service s3Service;

    @PreAuthorize("permitAll()")
    @GetMapping("/")
    public ResponseEntity<ApiResponse<String>> getPresignedGetUrl(@RequestParam("name") String filename) {
        return ResponseEntity.status(HttpStatus.OK).body(
            ApiResponse.success(HttpStatus.OK, "get presigned get url successfully", s3Service.getPresignedGetUrl(filename)));
    }
}