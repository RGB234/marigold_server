plugins {
    java
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "com.sns"
version = "0.0.1-SNAPSHOT"

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

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8") // Swagger
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    implementation("com.github.mwiede:jsch:2.27.2") // SSH tunneling (ED25519 support)
    implementation("mysql:mysql-connector-java:8.0.32") // DB driver
    // Auth
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.5")
    // Storage
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3:3.1.1")
    implementation("org.apache.tika:tika-core:2.9.0")
    // TSID
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.0")
    //
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Testcontainers
    // testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers:2.0.2")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("com.navercorp.fixturemonkey:fixture-monkey-starter:1.0.0")
    
    // Data Faker for Dummy Data Generation
    implementation("net.datafaker:datafaker:2.1.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    java {
        trimTrailingWhitespace()
        removeUnusedImports()
        googleJavaFormat()
        endWithNewline()
    }
}
