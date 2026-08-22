plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
}

repositories {
	// jscore is consumed from the local maven repository for now. This must be declared
	// before the dependencies block, because the Graal version probe below resolves at
	// configuration time.
	mavenLocal()
}

loom {
	mods {
		register("jscore-js-runtime") {
			sourceSet(sourceSets.main.get())
		}
	}
}

val jscoreCoord = "${providers.gradleProperty("jscore_group").get()}:" +
		"${providers.gradleProperty("jscore_name").get()}:" +
		"${providers.gradleProperty("jscore_version").get()}"

// Compile-time only. Read back whatever Graal version the pinned jscore was built against,
// so js-language is never compiled or bundled against a different Truffle than the one
// jscore ships. Runtime alignment is handled entirely by the "~" depends range in
// fabric.mod.json, so there is no version check at runtime.
val graal = provider {
	configurations.detachedConfiguration(dependencies.create(jscoreCoord))
			.incoming.resolutionResult.allComponents
			.mapNotNull { it.moduleVersion }
			.first { it.group == "org.graalvm.polyglot" && it.name == "polyglot" }
			.version
}
val graalVersion: String = graal.get()

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Loom 1.17 no longer creates modImplementation/modApi; mod dependencies go on the
	// regular configurations and are remapped via modCompileClasspathMapped. This matches
	// how jscore itself declares fabric-api.
	implementation(jscoreCoord)

	// polyglot and truffle-api arrive transitively through js-language. They are provided
	// by jscore at runtime, so they are compiled against but never included.
	implementation("org.graalvm.js:js-language:$graalVersion")

	// Nest only the JS-specific half of GraalJS inside this mod jar; jscore nests the
	// polyglot/Truffle host half. include is not transitive, so the whole resolved graph
	// has to be listed by hand.
	for (graalModule in listOf(
		"org.graalvm.js:js-language",
		"org.graalvm.regex:regex",
		"org.graalvm.shadowed:icu4j",
		"org.graalvm.shadowed:xz",
	)) {
		include("$graalModule:$graalVersion")
	}
}

tasks.processResources {
	val version = version
	val jscoreVersion = providers.gradleProperty("jscore_version").get()
	inputs.property("version", version)
	inputs.property("jscore_version", jscoreVersion)

	filesMatching("fabric.mod.json") {
		expand("version" to version, "jscore_version" to jscoreVersion)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	repositories {
		// Add repositories to publish to here.
	}
}
