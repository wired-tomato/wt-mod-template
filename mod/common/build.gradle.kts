plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("net.neoforged.moddev")
}

val neo_form_version: String by rootProject.properties
val parchment_minecraft: String by rootProject.properties
val parchment_version: String by rootProject.properties

neoForge {
    neoFormVersion = neo_form_version

    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.add(at.absolutePath)
    }

    parchment {
        minecraftVersion = parchment_minecraft
        mappingsVersion = parchment_version
    }
}

dependencies {
    compileOnly("org.spongepowered:mixin:0.8.5")
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")
}

sourceSets.main {
    resources {
        srcDir("src/main/generated/resources")
    }
}
