import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.wiredtomato.versioning.Version
import org.spongepowered.gradle.vanilla.MinecraftExtension

val cutoff = Version("1.20.2")
val minecraft_version: String by rootProject.properties
val versionRetriever = CommonVersionRetrievers.getOrCreate(minecraft_version)
val minecraftVersion = Version(minecraft_version)
val mod_id: String by rootProject.properties

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("net.neoforged.moddev") apply false
    id("org.spongepowered.gradle.vanilla") version "0.2.1-SNAPSHOT" apply false
}

if (minecraftVersion >= cutoff) {
    apply(plugin = "net.neoforged.moddev")

    val neo_form_version = versionRetriever.getLatestNeoformVersion()
    val parchment_minecraft = versionRetriever.minecraftVersion
    val parchment_version = versionRetriever.getLatestParchmentVersion()

    (project.extensions.getByName("neoForge") as NeoForgeExtension).apply {
        setNeoFormVersion(neo_form_version)

        val at = file("src/main/resources/META-INF/accesstransformer.cfg")
        if (at.exists()) {
            accessTransformers.from(at.absolutePath)
        }

        parchment {
            minecraftVersion = parchment_minecraft
            mappingsVersion = parchment_version
        }
    }
} else {
    apply(plugin = "org.spongepowered.gradle.vanilla")

    (project.extensions.getByName("minecraft") as MinecraftExtension).apply {
        version(minecraft_version)
        val aw = file("src/main/resources/${mod_id}.accesswidener")

        if (aw.exists()) {
            accessWideners(aw)
        }
    }
}

dependencies {
    compileOnly("org.spongepowered:mixin:0.8.7")
    compileOnly("io.github.llamalad7:mixinextras-common:0.4.1")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1")
}

sourceSets.main {
    resources {
        srcDirs(
            "src/main/generated/resources/client",
            "src/main/generated/resources/server",
        )
    }
}
