plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("fabric-loom")
}

val minecraft_version: String by project.properties
val parchment_minecraft: String by rootProject.properties
val parchment_version: String by rootProject.properties
val fabric_loader_version: String by rootProject.properties
val fabric_version: String by rootProject.properties
val flk_version: String by rootProject.properties
val mod_id: String by rootProject.properties
val mod_name: String by rootProject.properties
val mod_description: String by rootProject.properties
val mod_author: String by rootProject.properties
val license: String by rootProject.properties
val java_version: String by rootProject.properties

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-$parchment_minecraft:$parchment_version@zip")
    })

    modImplementation("net.fabricmc:fabric-loader:$fabric_loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")
    modImplementation("net.fabricmc:fabric-language-kotlin:$flk_version")

    compileOnly(project(":mod-common"))
}

loom {
    val aw = project(":mod-common").file("src/main/resources/$mod_id.accesswidener")
    if (aw.exists()) {
        accessWidenerPath.set(aw)
    }

    mixin {
        defaultRefmapName.set("$mod_id.refmap.json")
    }

    runs {
        val client by getting {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("runs/client")
        }

        val server by getting {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
            runDir("runs/server")
        }
    }
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.compileJava {
    val commonJava = project(":mod-common").tasks.compileJava.get()
    dependsOn(commonJava)
    source(commonJava.source)
}

tasks.compileKotlin {
    val commonKotlin = project(":mod-common").tasks.compileKotlin.get()
    dependsOn(commonKotlin)
    source(commonKotlin.sources)
}

tasks.processResources {
    val commonResources = project(":mod-common").tasks.processResources.get()
    dependsOn(commonResources)
    from(commonResources)
}

tasks.sourcesJar {
    val commonSources = project(":mod-common").tasks.getByName<Jar>("sourcesJar")
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}
