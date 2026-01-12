pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		maven("https://maven.shedaniel.me/")
		maven("https://maven.terraformersmc.com/releases/")
		maven("https://maven.fabricmc.net")
		maven("https://maven.architectury.dev")
		maven("https://maven.minecraftforge.net")
		maven("https://repo.essential.gg/repository/maven-public")
		maven("https://repo.spongepowered.org/maven/")
	}
}

includeBuild("../essential-gradle-toolkit")
rootProject.name = "SeymourAnalyzer"
rootProject.buildFileName = "root.gradle.kts"

listOf(
	"1.21.5-fabric",
	"1.21.8-fabric",
	"1.21.10-fabric",
	"1.21.11-fabric",
).forEach { version ->
	include(":$version")
	project(":$version").apply {
		projectDir = file("versions/$version")
		buildFileName = "../../build.gradle.kts"
	}
}
