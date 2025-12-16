plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
}

group = "com.ake3m.dns"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.bundles.logging)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.ake3m.dns.DNSForwarderMain"
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    version = ""
    archiveClassifier.set("")
}