package com.sns.marigold.global.validator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.global.annotation.ValidImageFiles;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.service.S3Service;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Component
public class ImageFilesValidatorForList
        implements ConstraintValidator<ValidImageFiles, List<MultipartFile>> {

    @Autowired
    private S3Service s3Service;

    @Override
    public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext ctx) {
        if (files == null || files.isEmpty()) {
            return true;
        }


        // 실제 내용이 있는 파일이 하나도 없는 경우 통과
        List<MultipartFile> nonEmptyFiles = files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .toList();

        if (nonEmptyFiles.isEmpty()) {
            return true;
        }

        // Tika로 실제 파일 바이트를 분석해 이미지인지 검증
        try {
            s3Service.validateRealImageFiles(nonEmptyFiles);
        } catch (StorageException e) {
            replaceMessage(ctx, "이미지 파일만 업로드 가능합니다. (jpg, png, gif, webp)");
            return false;
        }
        return true;
    }

    private void replaceMessage(ConstraintValidatorContext ctx, String message) {
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
