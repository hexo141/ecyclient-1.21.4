import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom-remap")
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
    mavenCentral()
}

val javafxVersion = "21"

dependencies {
    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    mappings("net.fabricmc:yarn:${providers.gradleProperty("yarn_mappings").get()}:v2")
    modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

    // JavaFX 基础模块（跨平台 API）
    include("org.openjfx:javafx-media:${javafxVersion}")
    include("org.openjfx:javafx-controls:${javafxVersion}")
    include("org.openjfx:javafx-graphics:${javafxVersion}")

    // Windows 64位
    include("org.openjfx:javafx-media:${javafxVersion}:win")
    include("org.openjfx:javafx-controls:${javafxVersion}:win")
    include("org.openjfx:javafx-graphics:${javafxVersion}:win")
    include("org.openjfx:javafx-base:${javafxVersion}:win")

    // Linux 64位
    include("org.openjfx:javafx-media:${javafxVersion}:linux")
    include("org.openjfx:javafx-controls:${javafxVersion}:linux")
    include("org.openjfx:javafx-graphics:${javafxVersion}:linux")
    include("org.openjfx:javafx-base:${javafxVersion}:linux")

    // Linux aarch64
    include("org.openjfx:javafx-media:${javafxVersion}:linux-aarch64")
    include("org.openjfx:javafx-controls:${javafxVersion}:linux-aarch64")
    include("org.openjfx:javafx-graphics:${javafxVersion}:linux-aarch64")
    include("org.openjfx:javafx-base:${javafxVersion}:linux-aarch64")
}

tasks.processResources {
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    inputs.property("projectName", project.name)

    from("LICENSE") {
        rename { "${it}_${project.name}" }
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        // 发布仓库配置
    }
}