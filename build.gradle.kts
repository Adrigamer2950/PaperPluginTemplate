@file:Suppress("VulnerableLibrariesLocal")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.hangarpublishplugin.model.Platforms
import xyz.jpenilla.runpaper.task.RunServer
import xyz.jpenilla.runtask.task.AbstractRun
import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm")
    alias(libs.plugins.shadow)
    alias(libs.plugins.plugin.yml)
    alias(libs.plugins.run.server)
    alias(libs.plugins.modrinth)
    alias(libs.plugins.hangar.publish)
}

val versionIsBeta = (properties["version"] as String).lowercase().contains("beta")

group = "me.adrigamer2950"
version = properties["version"] as String +
        if (versionIsBeta)
            "-${getGitCommitHash()}"
        else ""

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://repo.devadri.es/repository/releases") {
        name = "devadri"
    }
}

dependencies {
    compileOnly(libs.jetbrains.annotations)

    compileOnly(libs.paper.api)

    implementation(libs.adriapi)

    implementation(kotlin("stdlib-jdk8"))
}

val targetJavaVersion = (properties["java-version"] as String).toInt()

kotlin {
    jvmToolchain(targetJavaVersion)
}

bukkit {
    name = rootProject.name
    main = properties["main-class"] as String
    apiVersion = properties["api-version"] as String
    author = properties["author"] as String
    website = properties["website"] as String
    description = properties["description"] as String
    foliaSupported = true
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.jar {
    enabled = false
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")

    relocate("me.adrigamer2950.adriapi.api", "me.adrigamer2950.ppt.libs.adriapi")
}

tasks.withType<JavaCompile>().configureEach {
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release = targetJavaVersion
        options.encoding = "UTF-8"
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

fun getJarFile(): File? {
    val jarFile = File("./gh-assets").listFiles()?.firstOrNull { it.name.endsWith(".jar") }
    return jarFile
}

fun getGitCommitHash(): String {
    val byteOut = ByteArrayOutputStream()

    @Suppress("DEPRECATION")
    exec {
        commandLine = "git rev-parse --short HEAD".split(" ")
        standardOutput = byteOut
    }

    return String(byteOut.toByteArray()).trim()
}

modrinth {
    token = System.getenv("MODRINTH_TOKEN")
    projectId = properties["modrinth-id"] as String
    versionNumber = version as String
    versionName = rootProject.name + " " + version
    versionType = properties["modrinth-type"] as String
    uploadFile.set(getJarFile())

    gameVersions.set(
        (properties["modrinth-version"] as String)
            .split(",")
            .map { it.trim() }
    )

    val modrinthLoaders: List<String> = (properties["modrinth-loaders"] as String)
        .split(",")
        .map { it.trim() }
    loaders.set(modrinthLoaders)

    syncBodyFrom = rootProject.file("README.md").readText()
}

hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String)
        channel.set(properties["hangar-channel"] as String)
        id.set(properties["hangar-id"] as String)
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        platforms {
            register(Platforms.PAPER) {
                jar.set(getJarFile())

                val versions: List<String> = (properties["hangar-version"] as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }
        }
    }
}

tasks.named<RunServer>("runServer").configure {
    minecraftVersion("1.20.6")

    downloadPlugins {
        // ViaVersion
        hangar("ViaVersion", "5.2.0")
    }
}

tasks.withType(AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        @Suppress("UnstableApiUsage")
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs(
        // Hot Swap
        "-XX:+AllowEnhancedClassRedefinition",

        // Aikar Flags
        "--add-modules=jdk.incubator.vector", "-XX:+UseG1GC", "-XX:+ParallelRefProcEnabled",
        "-XX:MaxGCPauseMillis=200", "-XX:+UnlockExperimentalVMOptions", "-XX:+DisableExplicitGC",
        "-XX:+AlwaysPreTouch", "-XX:G1NewSizePercent=30", "-XX:G1MaxNewSizePercent=40",
        "-XX:G1HeapRegionSize=8M", "-XX:G1ReservePercent=20", "-XX:G1HeapWastePercent=5",
        "-XX:G1MixedGCCountTarget=4", "-XX:InitiatingHeapOccupancyPercent=15",
        "-XX:G1MixedGCLiveThresholdPercent=90", "-XX:G1RSetUpdatingPauseTimePercent=5",
        "-XX:SurvivorRatio=32", "-XX:+PerfDisableSharedMem", "-XX:MaxTenuringThreshold=1",
        "-Dusing.aikars.flags=https://mcflags.emc.gs", "-Daikars.new.flags=true"
    )

}
