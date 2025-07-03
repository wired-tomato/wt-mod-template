plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("net.neoforged.moddev")
}

val minecraft_version: String by rootProject.properties
val versionRetriever = CommonVersionRetrievers.getOrCreate(minecraft_version)
val parchment_minecraft = versionRetriever.minecraftVersion
val parchment_version = versionRetriever.getLatestParchmentVersion()
val neoforge_version = versionRetriever.getLatestNeoForgeVersion()
val kff_version = versionRetriever.getLatestKotlinForNeoForgeVersion()
val mod_id: String by rootProject.properties

val common = project(":common")

repositories {
    maven("https://thedarkcolour.github.io/KotlinForForge") {
        name = "Kotlin for Forge"
    }
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:$kff_version")
    compileOnly(common)
}


val generatedResources = common.file("src/main/generated/resources")
val existingResources: File = common.file("src/main/resources")

neoForge {
    version = neoforge_version

    val at = common.file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }

    parchment {
        minecraftVersion = parchment_minecraft
        mappingsVersion = parchment_version
    }

    runs {
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", mod_id)
            ideName = "NeoForge ${name.replaceFirstChar { it.titlecase() }} (${project.path})" // Unify the run config names with fabric
        }

        val client by creating {
            client()
        }

        val clientData by creating {
            clientData()
            programArguments.addAll("--all", "--mod", rootProject.property("mod_id").toString())
            programArguments.addAll("--output", generatedResources.resolve("client").absolutePath)
            programArguments.addAll("--existing", existingResources.absolutePath)
        }

        val serverData by creating {
            serverData()
            programArguments.addAll("--all", "--mod", rootProject.property("mod_id").toString())
            programArguments.addAll("--output", generatedResources.resolve("server").absolutePath)
            programArguments.addAll("--existing", existingResources.absolutePath)
        }

        val server by creating {
            server()
        }
    }

    mods {
        create(mod_id) {
            sourceSet(sourceSets["main"])
        }
    }
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
