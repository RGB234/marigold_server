plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "com.sns"
version = "0.0.1-SNAPSHOT"

extra["jackson-bom.version"] = "2.21.7"
extra["netty.version"] = "4.1.137.Final"
extra["tomcat.version"] = "10.1.60"
extra["commons-lang3.version"] = "3.20.0"
extra["log4j2.version"] = "2.25.5"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

// 기본 test 태스크는 빠르게 실행할 수 있는 단위 테스트만 담당한다.
sourceSets.test {
    java.setSrcDirs(listOf("src/test/unit/java"))
    resources.setSrcDirs(listOf("src/test/unit/resources"))
}

// Spring 일부 계층을 함께 검증하는 컴포넌트 테스트를 별도 소스셋으로 분리한다.
val componentTest by sourceSets.creating {
    java.setSrcDirs(listOf("src/test/component/java"))
    resources.setSrcDirs(listOf("src/test/component/resources"))
}

// 외부 인프라와 전체 Spring 컨텍스트를 사용하는 통합 테스트를 별도 소스셋으로 분리한다.
val integrationTest by sourceSets.creating {
    java.setSrcDirs(listOf("src/test/integration/java"))
    resources.setSrcDirs(listOf("src/test/integration/resources"))
}

// 별도 테스트 소스셋에서도 운영 코드와 공통 테스트 라이브러리를 재사용한다.
listOf(componentTest, integrationTest).forEach { suite ->
    suite.compileClasspath += sourceSets.main.get().output
    suite.runtimeClasspath += sourceSets.main.get().output
    configurations[suite.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
    configurations[suite.compileOnlyConfigurationName].extendsFrom(configurations.testCompileOnly.get())
    configurations[suite.annotationProcessorConfigurationName].extendsFrom(configurations.testAnnotationProcessor.get())
    configurations[suite.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17") // OpenAPI / Swagger UI
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    runtimeOnly("com.mysql:mysql-connector-j") // DB driver
    // Auth
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-redis") // 인증 세션·최근 인증 저장소
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    // Storage
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3:3.4.2")
    implementation("org.apache.tika:tika-core:3.2.2")
    // TSID
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.0")
    //
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // Docker가 필요한 Testcontainers 의존성은 통합 테스트에만 제공한다.
    add(integrationTest.implementationConfigurationName, "org.testcontainers:testcontainers:2.0.2") // testContainers가 사용하는 docker api와 최신 도커 환경과의 호환성
    add(integrationTest.implementationConfigurationName, "org.testcontainers:junit-jupiter")
    add(integrationTest.implementationConfigurationName, "org.testcontainers:mysql")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 컴포넌트 테스트 소스셋을 실행하는 독립 태스크다.
val componentTestTask = tasks.register<Test>("componentTest") {
    description = "Runs component tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = componentTest.output.classesDirs
    classpath = componentTest.runtimeClasspath
    shouldRunAfter(tasks.test)
}

// 통합 테스트 소스셋을 실행하며, 전체 검사에서는 가장 마지막에 실행되도록 한다.
val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    shouldRunAfter(tasks.test, componentTestTask)
}

// check 실행 시 단위 테스트뿐 아니라 컴포넌트·통합 테스트도 모두 검증한다.
tasks.check {
    dependsOn(componentTestTask, integrationTestTask)
}

tasks.register<JavaExec>("generateValidationPolicy") {
    group = "build setup"
    description = "Generates validation-policy.json from ValidationPolicy.java"
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.sns.marigold.global.validation.ValidationPolicyJsonGenerator")
    args(layout.projectDirectory.file("src/main/resources/validation-policy.json").asFile.absolutePath)
}

spotless {
    java {
        trimTrailingWhitespace()
        removeUnusedImports()
        googleJavaFormat()
        endWithNewline()
        importOrder("java", "javax", "org", "com")
    }
}
