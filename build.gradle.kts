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

dependencies {
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	mappings("net.fabricmc:yarn:${providers.gradleProperty("yarn_mappings").get()}:v2")
	modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

	// JavaFX 基础 API 依赖（编译时需要）
	modImplementation("org.openjfx:javafx-base:21")
	modImplementation("org.openjfx:javafx-controls:21")
	modImplementation("org.openjfx:javafx-graphics:21")
	modImplementation("org.openjfx:javafx-media:21")

	// JSON 处理依赖
	modImplementation("com.google.code.gson:gson:2.10.1")
	include("com.google.code.gson:gson:2.10.1")

	// 平台特定实现 - 使用 include 打包
	val javafxVersion = "21"
	
	// Windows 64-bit
	include(implementation("org.openjfx:javafx-base:${javafxVersion}:win")!!)
	include(implementation("org.openjfx:javafx-controls:${javafxVersion}:win")!!)
	include(implementation("org.openjfx:javafx-graphics:${javafxVersion}:win")!!)
	include(implementation("org.openjfx:javafx-media:${javafxVersion}:win")!!)
	
	// Linux 64-bit
	include(implementation("org.openjfx:javafx-base:${javafxVersion}:linux")!!)
	include(implementation("org.openjfx:javafx-controls:${javafxVersion}:linux")!!)
	include(implementation("org.openjfx:javafx-graphics:${javafxVersion}:linux")!!)
	include(implementation("org.openjfx:javafx-media:${javafxVersion}:linux")!!)
	
	// Linux aarch64
	include(implementation("org.openjfx:javafx-base:${javafxVersion}:linux-aarch64")!!)
	include(implementation("org.openjfx:javafx-controls:${javafxVersion}:linux-aarch64")!!)
	include(implementation("org.openjfx:javafx-graphics:${javafxVersion}:linux-aarch64")!!)
	include(implementation("org.openjfx:javafx-media:${javafxVersion}:linux-aarch64")!!)
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
}