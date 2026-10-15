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
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("su.nightexpress.excellentcrates:crates-api:$excellentCratesVersion")
    compileOnly("su.nightexpress.nightcore:main:$nightcoreVersion")
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

tasks.runServer {
    minecraftVersion("1.21.9")
    downloadPlugins {
        modrinth("nightcore", nightcoreVersion)
        modrinth("excellentcrates", excellentCratesVersion)
        // ExcellentCrates draws crate holograms through packetevents.
        modrinth("packetevents", "2.14.0+spigot")
    }
}
