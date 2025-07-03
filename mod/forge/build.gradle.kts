plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("net.minecraftforge.gradle") version "[6.0.24,6.2)"
}

val common = project(":common")

val minecraft_version: String by rootProject.properties
val versionRetriever = CommonVersionRetrievers.getOrCreate(minecraft_version)
val forge_version = versionRetriever.getLatestForgeVersion()
val kff_version = versionRetriever.getLatestKotlinForForgeVersion()
val parchment_minecraft = versionRetriever.minecraftVersion
val parchment_version = versionRetriever.getLatestParchmentVersion()

minecraft {
    mappings("official", minecraft_version)

    reobf = false
    copyIdeResources = true
}

repositories {
    mavenCentral()
    maven("https://thedarkcolour.github.io/KotlinForForge") {
        name = "Kotlin for Forge"
    }
    maven("https://maven.minecraftforge.net/")
}

dependencies {
    minecraft("net.minecraftforge:forge:${minecraft_version}-${forge_version}")
    implementation("thedarkcolour:kotlinforforge:$kff_version")
    compileOnly(common)
}

sourceSets.main {
    resources {
        srcDir("src/generated/resources")
    }
}

tasks.compileJava {
    val commonJava = common.tasks.compileJava.get()
    dependsOn(commonJava)
    source(commonJava.source)
}

tasks.compileKotlin {
    val commonKotlin = common.tasks.compileKotlin.get()
    dependsOn(commonKotlin)
    source(commonKotlin.sources)
}

tasks.processResources {
    val commonResources = common.tasks.processResources.get()
    dependsOn(commonResources)
    from(commonResources)
}

tasks.sourcesJar {
    val commonSources = common.tasks.getByName<Jar>("sourcesJar")
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}
