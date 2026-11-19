plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.carlos"
version = "2.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
    options.encoding = "UTF-8"
}

// The workspace lives on an exFAT volume where macOS writes "._*" AppleDouble files next to every
// file; they vanish while Gradle cleans its output folders and make the build fail. Build elsewhere:
//   ./gradlew test -PbuildDir=/tmp/archipelagos-build
providers.gradleProperty("buildDir").orNull?.let { layout.buildDirectory.set(file(it)) }

tasks.processResources {
    exclude("**/._*")
}

tasks.test {
    useJUnitPlatform()
}
