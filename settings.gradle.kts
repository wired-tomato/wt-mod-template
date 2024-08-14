pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven("https://maven.fabricmc.net") {
                    name = "Fabric"
                }
            }

            filter {
                includeGroup("net.fabricmc")
                includeGroup("fabric-loom")
            }
        }

        exclusiveContent {
            forRepository {
                maven("https://repo.spongepowered.org/repository/maven-public/") {
                    name = "Sponge"
                }
            }

            filter {
                includeGroupAndSubgroups("org.spongepowered")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("deplatformed-api", "deplatformed-ksp")
project(":deplatformed-api").projectDir = file("deplatformed/api")
project(":deplatformed-ksp").projectDir = file("deplatformed/ksp")

include("mod-common", "mod-fabric", "mod-neoforge")
project(":mod-common").projectDir = file("mod/common")
project(":mod-fabric").projectDir = file("mod/fabric")
project(":mod-neoforge").projectDir = file("mod/neoforge")

rootProject.name = "wt-mod-template"

