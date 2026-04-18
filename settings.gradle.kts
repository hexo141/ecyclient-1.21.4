pluginManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://repository.hanbings.io/proxy")
		}
		mavenCentral()
		gradlePluginPortal()
	}

	plugins {
		id("net.fabricmc.fabric-loom-remap") version providers.gradleProperty("loom_version")
	}
}

// Should match your modid
rootProject.name = "ecyclient"
