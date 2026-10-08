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
    
    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:2.0.2") // testContainers가 사용하는 docker api와 최신 도커 환경과의 호환성
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
}

tasks.withType<Test> {
    useJUnitPlatform()
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
