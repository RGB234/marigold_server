package com.sns.marigold.global.config;

import com.sns.marigold.adoption.dto.AdoptionPostCreateDto;
import com.sns.marigold.adoption.enums.*; // Enum 한 번에 import
import com.sns.marigold.adoption.service.AdoptionPostService;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.user.dto.create.UserCreateDto;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@Commit
public class DebugServiceTest {

    @Autowired private UserService userService;
    @Autowired private AdoptionPostService adoptionPostService;
    private final Logger logger = LoggerFactory.getLogger(DebugServiceTest.class);

    @Test
    @DisplayName("개발용 초기 데이터 생성 스크립트")
    void runDebugLogic() {
        System.out.println("============== Debug Data Seeding Start ==============");

        // 1. 사용자 확보 (User & Admin)
        Long user1Id = getOrCreateUser(ProviderInfo.NAVER, "wN78Zkln2kMk6Yzr6fHELw191jMCGCD2reEWjr0-gpU", Role.ROLE_PERSON);
        Long adminId = getOrCreateUser(ProviderInfo.KAKAO, "4447943543", Role.ROLE_ADMIN);

        try {
            // 2. 게시글 생성
            createAdoption(user1Id, "햄스터.png", "함츄쵸", Species.RODENTS, Sex.MALE, Neutering.UNKNOWN, "쌍문동");
            createAdoption(user1Id, "코양이.jpg", "캣시키", Species.CAT, Sex.FEMALE, Neutering.YES, "용인시");
            createAdoption(adminId, "가나디.jpg", "가나디", Species.DOG, Sex.MALE, Neutering.YES, "안산시");
            createAdoption(adminId, "크리스탈레드쉬림프.jpg", "열대어", Species.FISH, Sex.UNKNOWN, Neutering.NO, "고양시");

        } catch (Exception e) {
            System.err.println("❌ 데이터 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Debug Data Seeding Failed", e);
        }

        System.out.println("============== Debug Data Seeding End ==============");
    }

    // --- Helper Methods ---

    // 사용자 생성/조회 (멱등성 보장)
    private Long getOrCreateUser(ProviderInfo provider, String providerId, Role role) {
        try {
            Long userId = userService.createUser(UserCreateDto.builder()
                    .providerInfo(provider)
                    .providerId(providerId)
                    .role(role)
                    .build());
            logger.info("✅ 사용자 생성 완료: " + provider + " " + providerId);
            return userId;
        } catch (UserException e) {
            logger.info("ℹ️ 사용자 이미 존재: " + provider + " " + providerId);
            return userService.findEntityByProviderInfoAndProviderId(provider, providerId)
                    .orElseThrow()
                    .getId();
        }
    }

    // 입양 공고 생성 헬퍼
    private void createAdoption(Long userId, String fileName, String title, Species species, Sex sex, Neutering neutering, String area) throws IOException {
        // 파일 읽기 (NIO Files 사용으로 Stream 관리 자동화)
        Path path = Paths.get("src/test/resources/images/" + fileName);
        byte[] content = Files.readAllBytes(path);

        // MockMultipartFile 생성
        MultipartFile imageFile = new MockMultipartFile(
                "images",        // DTO 필드명
                fileName,        // 원본 파일명
                getContentType(fileName), // 확장자에 따른 타입 추론
                content
        );

        // 서비스 호출
        adoptionPostService.create(AdoptionPostCreateDto.builder()
                .title(title)
                .species(species)
                .sex(sex)
                .neutering(neutering)
                .area(area)
                .age(2)              // 테스트용 고정값
                .weight(1.5)         // 테스트용 고정값
                .features(title + "입니다.") // 테스트용
                .images(List.of(imageFile))
                .build(), Objects.requireNonNull(userId));
        
        System.out.println("✅ 게시글 생성 완료: " + title);
    }

    // 확장자로 Content-Type 결정
    private String getContentType(String filename) {
        if (filename.endsWith(".png")) return "image/png";
        return "image/jpeg";
    }
}