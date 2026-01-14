package com.sns.marigold.user.dto.update;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter // @ModelAttribute 사용 시 필요
@NoArgsConstructor
public class UserUpdateDto {
  @Size(min = 3, max = 12)
  private String nickname;

  @Schema(description = "업로드할 이미지 파일", type = "string", format = "binary")
  private MultipartFile image;

  @Schema(description = "이미지 삭제 여부", type = "boolean")
  private boolean removeImage = false;
}
