plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("net.neoforged.moddev")
}

val minecraft_version: String by rootProject.properties
val neo_form_version = VersionRetriever.getLatestNeoformVersion(minecraft_version)
val parchment_minecraft: String by rootProject.properties
val parchment_version: String by rootProject.properties

neoForge {
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
