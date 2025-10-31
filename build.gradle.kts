plugins {
    id("java")
}

group = "io.github.krys"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.6")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}