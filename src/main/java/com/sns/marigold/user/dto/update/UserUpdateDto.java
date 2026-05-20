package com.sns.marigold.user.dto.update;

import com.sns.marigold.global.annotation.ValidImageFiles;
import com.sns.marigold.global.validation.ValidationPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter // @ModelAttribute 사용 시 필요
@NoArgsConstructor
public class UserUpdateDto {
  @Size(
      min = ValidationPolicy.User.NICKNAME_MIN_LENGTH,
      max = ValidationPolicy.User.NICKNAME_MAX_LENGTH,
      message = "닉네임은 2자 이상 12자 이하로 구성해야 합니다.")
  @Pattern(
      regexp = ValidationPolicy.User.NICKNAME_ALLOWED_PATTERN,
      message = "닉네임은 영문자, 한글, 숫자만 사용할 수 있습니다.")
  private String nickname;

  @Schema(description = "업로드할 이미지 파일", type = "string", format = "binary")
  @ValidImageFiles
  private MultipartFile image;

  @Schema(description = "이미지 삭제 여부", type = "boolean")
  private boolean removeImage = false;
}
