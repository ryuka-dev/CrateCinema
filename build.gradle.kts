plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

val paperApiVersion = providers.gradleProperty("paperApiVersion").get()
val excellentCratesVersion = providers.gradleProperty("excellentCratesVersion").get()
val nightcoreVersion = providers.gradleProperty("nightcoreVersion").get()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // ExcellentCrates and nightcore are published here.
    maven("https://repo.nightexpressdev.com/releases")
    // Floodgate API, for telling Bedrock players apart.
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("su.nightexpress.excellentcrates:crates-api:$excellentCratesVersion")
    compileOnly("su.nightexpress.nightcore:main:$nightcoreVersion")
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")
}

// ExcellentCrates 7.x is compiled for Java 25 (class file version 69), so servers running it
// are on Java 25 already and its API cannot be compiled against with an older javac.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.add("-Xlint:all")
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

// -PtestMc=<version> runs the test server on another Minecraft version, in its own run-<version>
// directory: a world upgraded to a newer version cannot be opened by an older one.
val testMc = providers.gradleProperty("testMc").orNull
// -PtestEcJar=<path> runs the test server with that ExcellentCrates jar instead of the Modrinth
// release, for checking a build that is not published yet. Pair it with -PtestRun=<directory>:
// a newer ExcellentCrates may migrate its data so that the release can no longer read it.
val testEcJar = providers.gradleProperty("testEcJar").orNull
val testRun = providers.gradleProperty("testRun").orNull

tasks.runServer {
    minecraftVersion(testMc ?: "1.21.9")
    val runDir = testRun ?: testMc?.let { "run-$it" }
    if (runDir != null) {
        runDirectory.set(layout.projectDirectory.dir(runDir))
    }
    if (testEcJar != null) {
        pluginJars(file(testEcJar))
    }
    downloadPlugins {
        modrinth("nightcore", nightcoreVersion)
        if (testEcJar == null) {
            modrinth("excellentcrates", excellentCratesVersion)
        }
        // ExcellentCrates draws crate holograms through packetevents.
        modrinth("packetevents", "2.14.0+spigot")
    }
}
