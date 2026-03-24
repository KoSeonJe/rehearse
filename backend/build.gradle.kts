plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.rehearse"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Database
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")

    // HikariCP 6.x (Virtual Thread pinning 방지)
    implementation("com.zaxxer:HikariCP:6.2.1")

    // Resilience4j RateLimiter (외부 API 동시성 제어)
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Spring Retry (낙관적 잠금 재시도)
    implementation("org.springframework.retry:spring-retry")

    // Flyway (DB 마이그레이션)
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // AWS S3
    implementation(platform("software.amazon.awssdk:bom:2.29.51"))
    implementation("software.amazon.awssdk:s3")

    // PDF
    implementation("org.apache.pdfbox:pdfbox:3.0.4")

    // .env 파일 로드
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs("-Djdk.tracePinnedThreads=short")
}
