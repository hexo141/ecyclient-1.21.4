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

// 创建独立的 JavaFX 配置，避免被 Loom 处理
val javafx by configurations.creating {
    isTransitive = false
}

// 让 compileClasspath 和 runtimeClasspath 包含 JavaFX
configurations {
    compileClasspath.get().extendsFrom(javafx)
    runtimeClasspath.get().extendsFrom(javafx)
}

dependencies {
    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    mappings("net.fabricmc:yarn:${providers.gradleProperty("yarn_mappings").get()}:v2")
    modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

    // JavaFX 依赖添加到独立配置中
    javafx("org.openjfx:javafx-media:${javafxVersion}")
    javafx("org.openjfx:javafx-controls:${javafxVersion}")
    javafx("org.openjfx:javafx-graphics:${javafxVersion}")
    javafx("org.openjfx:javafx-base:${javafxVersion}")

    // Windows 64位
    javafx("org.openjfx:javafx-media:${javafxVersion}:win")
    javafx("org.openjfx:javafx-controls:${javafxVersion}:win")
    javafx("org.openjfx:javafx-graphics:${javafxVersion}:win")
    javafx("org.openjfx:javafx-base:${javafxVersion}:win")

    // Linux 64位
    javafx("org.openjfx:javafx-media:${javafxVersion}:linux")
    javafx("org.openjfx:javafx-controls:${javafxVersion}:linux")
    javafx("org.openjfx:javafx-graphics:${javafxVersion}:linux")
    javafx("org.openjfx:javafx-base:${javafxVersion}:linux")

    // Linux aarch64
    javafx("org.openjfx:javafx-media:${javafxVersion}:linux-aarch64")
    javafx("org.openjfx:javafx-controls:${javafxVersion}:linux-aarch64")
    javafx("org.openjfx:javafx-graphics:${javafxVersion}:linux-aarch64")
    javafx("org.openjfx:javafx-base:${javafxVersion}:linux-aarch64")
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