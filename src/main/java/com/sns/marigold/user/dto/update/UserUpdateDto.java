package com.sns.marigold.user.dto.update;

import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.global.annotation.ValidImageFiles;
import com.sns.marigold.global.validation.ValidationPolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter // @ModelAttribute 사용 시 필요
@NoArgsConstructor
@Schema(description = "사용자 프로필 수정 요청")
public class UserUpdateDto {
  @Schema(description = "변경할 닉네임. 영문자, 한글, 숫자만 허용", example = "새닉네임")
  @Size(
      min = ValidationPolicy.User.NICKNAME_MIN_LENGTH,
      max = ValidationPolicy.User.NICKNAME_MAX_LENGTH,
      message = "닉네임은 2자 이상 12자 이하로 구성해야 합니다.")
  @Pattern(
      regexp = ValidationPolicy.User.NICKNAME_ALLOWED_PATTERN,
      message = "닉네임은 영문자, 한글, 숫자만 사용할 수 있습니다.")
  private String nickname;

  @Schema(description = "새 프로필 이미지 파일", type = "string", format = "binary")
  @ValidImageFiles
  private MultipartFile image;

  @Schema(description = "현재 프로필 이미지 삭제 여부", type = "boolean", example = "false")
  private boolean removeImage = false;
}
