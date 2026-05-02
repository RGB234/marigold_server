package com.sns.marigold.adoption.dto;

import com.sns.marigold.global.annotation.ValidImageCount;
import com.sns.marigold.global.annotation.ValidImageFiles;
import com.sns.marigold.global.validator.ImageCountValidatable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidImageCount(min = 0, max = 1)
public class AdoptionCommentUpdateDto implements ImageCountValidatable {

  @NotBlank(message = "내용이 비어있습니다.")
  @Size(max = 1000, message = "댓글은 1000자 이하여야 합니다.")
  private String content;

  @Schema(description = "기존 댓글 이미지를 삭제할지 여부", defaultValue = "false")
  private Boolean removeImage;

  @Schema(description = "새로 업로드할 댓글 이미지 파일 (최대 1개)", type = "string", format = "binary")
  @ValidImageFiles
  private List<MultipartFile> images;

  public boolean shouldRemoveImage() {
    return Boolean.TRUE.equals(removeImage);
  }

  public boolean hasNewImages() {
    return getImages().stream().anyMatch(file -> file != null && !file.isEmpty());
  }

  public List<String> getImagesToKeep() {
    return Collections.emptyList();
  }

  @Override
  public List<MultipartFile> getImages() {
    return images != null ? images : Collections.emptyList();
  }
}
