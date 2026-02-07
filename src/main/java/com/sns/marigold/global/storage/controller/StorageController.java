package com.sns.marigold.global.storage.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sns.marigold.global.storage.service.S3Service;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
class StorageController {
    private final S3Service s3Service;

    @GetMapping("/")
    public ResponseEntity<String> getPresignedGetUrl(@RequestParam("storeFileName") String storeFileName) {
        return ResponseEntity.ok(s3Service.getPresignedGetUrl(storeFileName));
    }
}