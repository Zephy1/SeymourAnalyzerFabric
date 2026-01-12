plugins {
	kotlin("jvm")
	id("maven-publish")
	id("gg.essential.multi-version")
	id("gg.essential.defaults")
}

version = property("mod_version").toString()
group = property("maven_group").toString()

repositories {
	maven("https://maven.shedaniel.me/")
	maven("https://maven.terraformersmc.com/releases")
}

dependencies {
	// Change versions in the global gradle.properties file
	val fabricLoaderVersion = libs.versions.fabricloader.get()
	val fabricKotlinVersion = libs.versions.fabrickotlin.get()
	modImplementation("net.fabricmc:fabric-loader:${fabricLoaderVersion}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${fabricKotlinVersion}")

	// Change versions in each project's gradle.properties file
	val fabricApiVersion = project.findProperty("fabric_api_version").toString()
	val clothConfigVersion = project.findProperty("cloth_config_version").toString()
	modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}") {
		exclude(group = "net.fabricmc.fabric-api", module = "fabric-content-registries-v0")
	}
	modApi("me.shedaniel.cloth:cloth-config-fabric:${clothConfigVersion}") {
		exclude(group = "net.fabricmc.fabric-api")
	}
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
	inputs.property("fabric_kotlin_version", libs.versions.fabrickotlin.get())

    filesMatching("fabric.mod.json") {
		expand(mapOf(
			"version" to inputs.properties["version"],
			"fabric_kotlin_version" to inputs.properties["fabric_kotlin_version"],
		))
    }
}

java {
	withSourcesJar()
}

tasks.named<Jar>("jar") {
    inputs.property("archivesName", base.archivesName)

    from("LICENSE") {
        rename { "${it}_${inputs.properties["archivesName"]}" }
    }
}

tasks.register<Copy>("collectJars") {
	val outputDir = projectDir.resolve("../../jars").normalize()
	dependsOn("remapJar")

	val mcVersion = project.findProperty("minecraft")?.toString()
		?: project.findProperty("yarn")?.toString()?.split("+")?.firstOrNull()
		?: "unknown"

	from(tasks.named("remapJar")) {
		include("*.jar")
		exclude("*-all.jar")

		exclude { fileTreeElement ->
			fileTreeElement.name.contains(" 1.1")
		}

		rename { fileName ->
			fileName
				.replace(".jar", "-$mcVersion.jar")
				.replace(" ", "-")
		}
	}
	into(outputDir)
}
tasks.named("build") {
	finalizedBy("collectJars")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = properties["archives_base_name"] as String
            from(components["java"])
        }
    }
}
