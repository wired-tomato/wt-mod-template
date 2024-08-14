plugins {
    kotlin("jvm") version "2.0.10"
    `maven-publish`
}

group = "net.wiredtomato"
base.archivesName = "deplatformed-ksp"
version = project.property("version").toString()

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.0-1.0.24")
    implementation("com.squareup:kotlinpoet:1.18.1")
    implementation("com.squareup:kotlinpoet-ksp:1.18.1")
    implementation("com.google.guava:guava:33.2.1-jre")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
