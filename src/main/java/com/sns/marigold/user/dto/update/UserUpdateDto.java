package com.sns.marigold.user.dto.update;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter // @ModelAttribute 사용 시 필요
@NoArgsConstructor
public class UserUpdateDto {
  @Size(min = 2, max = 12)
  @Pattern(regexp = "^[a-zA-Z가-힣0-9]+$", message = "닉네임은 영문자, 한글, 숫자만 사용할 수 있습니다.")
  private String nickname;

  @Schema(description = "업로드할 이미지 파일", type = "string", format = "binary")
  private MultipartFile image;

  @Schema(description = "이미지 삭제 여부", type = "boolean")
  private boolean removeImage = false;
}
