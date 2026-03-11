package com.sns.marigold.storage.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sns.marigold.global.dto.ApiResponse;
import com.sns.marigold.storage.service.S3Service;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
class StorageController {
    private final S3Service s3Service;

    @GetMapping("/")
    public ResponseEntity<ApiResponse<String>> getPresignedGetUrl(@RequestParam("name") String filename) {
        return ResponseEntity.status(HttpStatus.OK).body(
            ApiResponse.success(HttpStatus.OK, "get presigned get url successfully", s3Service.getPresignedGetUrl(filename)));
    }
}