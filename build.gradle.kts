import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
}

group = "no.nav.syfo"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

val tokenSupportVersion = "5.0.37"
val mockkVersion = "1.14.6"
val kotestVersion = "6.0.5"
val wiremockVersion = "3.13.2"
val wiremockKotestExtensionVersion = "3.1.0"
val hikariVersion = "6.3.2"
val logstashLogbackEncoderVersion = "7.4"
val owaspVersion = "20240325.1"
val apacheCommonsTextVersion = "1.14.0"
val detektVersion = "1.23.8"
val jedisVersion = "5.2.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.hibernate.validator:hibernate-validator")
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:$owaspVersion")
    implementation("org.apache.commons:commons-text:$apacheCommonsTextVersion")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")
    implementation("javax.inject:javax.inject:1")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("junit:junit")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("com.h2database:h2")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-wiremock:$wiremockKotestExtensionVersion")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")

    // Cache
    implementation("redis.clients:jedis:$jedisVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    config.from("detekt-config.yml")
    buildUponDefaultConfig = true
}
