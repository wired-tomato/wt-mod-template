import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlParser
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.until
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object VersionRetriever {
    private var cache = VersionCache("", Clock.System.now())
    private const val CACHE_FILE = "gradle/version_cache.json"
    private val json = Json {
        prettyPrint = true
    }

    private fun getMavenMetadata(mavenUrl: String, depPackage: String, module: String): Node {
        val url = if (mavenUrl.startsWith("https://") || mavenUrl.startsWith("http://")) {
            if (mavenUrl.endsWith("/")) {
                mavenUrl.removeSuffix("/")
            } else mavenUrl
        } else if (mavenUrl.endsWith("/")) {
            mavenUrl.removeSuffix("/")
        } else "https://$mavenUrl"

        val httpClient = HttpClient(CIO)
        val metadataResponse = runBlocking {
            httpClient.get("$url/${depPackage.replace(".", "/")}/$module/maven-metadata.xml")
        }

        if (metadataResponse.status != HttpStatusCode.OK) {
            throw IllegalStateException("Failed to retrieve versions for $module in $url")
        }

        val metadataString = runBlocking {
            metadataResponse.bodyAsText()
        }

        val parser = XmlParser()
        val metadata = parser.parse(StringReader(metadataString))

        return metadata
    }

    private fun getLatestVersion(mavenUrl: String, depPackage: String, module: String): String {
        val metadata = getMavenMetadata(mavenUrl, depPackage, module)
        val versioning = metadata.get("versioning") as NodeList
        val latest = ((versioning.getAt("latest").first as Node).value() as List<*>).first().toString()
        return latest
    }

    private fun getVersions(mavenUrl: String, depPackage: String, module: String): List<String> {
        val metadata = getMavenMetadata(mavenUrl, depPackage, module)
        val versioning = metadata.get("versioning") as NodeList
        val versions = versioning.getAt("versions") as NodeList
        val values = versions.getAt("version").map {
            ((it as Node).value() as List<*>).first().toString()
        }

        return values
    }

    private fun ensureUpToDateCache(minecraftVersion: String) {
        if (cache.minecraftVersion != minecraftVersion) {
            updateCaches(minecraftVersion)
        }
    }

    fun getLatestNeoformVersion(minecraftVersion: String): String {
        ensureUpToDateCache(minecraftVersion)
        return cache.versions.find { it.first == MavenModule.NEOFORM }!!.second.version
    }

    private fun fetchLatestNeoformVersion(minecraftVersion: String): String {
        val versions = getVersions("maven.neoforged.net", "net.neoforged", "neoform")

        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss")

        val latest = versions.mapNotNull { version ->
            val split = version.split("-")
            if (split.size == 1) return@mapNotNull null

            val mcVersion = split.subList(0, split.size - 1).joinToString("-")
            val date = LocalDateTime.parse(split.last(), dateTimeFormatter)
            mcVersion to date
        }.filter {
            it.first == minecraftVersion
        }.maxByOrNull { it.second }

        if (latest == null) return ""

        return listOf(latest.first, latest.second.format(dateTimeFormatter)).joinToString("-")
    }

    fun getLatestNeoForgeVersion(minecraftVersion: String): String {
        ensureUpToDateCache(minecraftVersion)
        return cache.versions.find { it.first == MavenModule.NEOFORGE }!!.second.version
    }

    private fun fetchLatestNeoForgeVersion(minecraftVersion: String): String {
        val versions = getVersions("maven.neoforged.net", "net.neoforged", "neoforge")

        val latest = versions.map { version ->
            val split = version.split(".")
            val mcVersion = split.subList(0, split.size - 1).joinToString(".")
            val nfVersion = split.last()
            mcVersion to nfVersion
        }.filter {
            if (it.first.startsWith("0.")) {
                it.first.removePrefix("0.") == minecraftVersion
            } else "1.${it.first}" == minecraftVersion
        }.maxByOrNull { it.second.split("-").first().toInt() }

        if (latest == null) return ""

        return listOf(latest.first, latest.second).joinToString("-")
    }

    fun getLatestFabricLoaderVersion(minecraftVersion: String): String {
        ensureUpToDateCache(minecraftVersion)
        return cache.versions.find { it.first == MavenModule.FABRIC_LOADER }!!.second.version
    }

    private fun fetchLatestFabricLoaderVersion(): String {
        return getLatestVersion("maven.fabricmc.net", "net.fabricmc", "fabric-loader")
    }

    fun getLatestFabricApiVersion(minecraftVersion: String): String {
        ensureUpToDateCache(minecraftVersion)
        return cache.versions.find { it.first == MavenModule.FABRIC_API }!!.second.version
    }

    private fun fetchLatestFabricApiVersion(minecraftVersion: String): String {
        val versions = getVersions("maven.fabricmc.net", "net.fabricmc.fabric-api", "fabric-api")

        val latest = versions.mapNotNull { version ->
            if (version.contains("build")) return@mapNotNull null

            val split = version.split("+")
            val mcVersion = split.last()
            val apiVersion = Version.parse(split.first())
            mcVersion to apiVersion
        }.filter {
            it.first == minecraftVersion
        }.maxByOrNull { it.second }

        if (latest == null) return ""

        return listOf(latest.second, latest.first).joinToString("+")
    }

    fun getLatestFabricLangKotlinVersion(minecraftVersion: String): String {
        ensureUpToDateCache(minecraftVersion)
        return cache.versions.find { it.first == MavenModule.FABRIC_LANG_KOTLIN }!!.second.version
    }

    private fun fetchLatestFabricLangKotlinVersion(): String {
        return getLatestVersion("maven.fabricmc.net", "net.fabricmc", "fabric-language-kotlin")
    }

    fun getLatestKotlinForNeoForgeVersion(minecraftVersion: String): String {
        ensureUpToDateCache(minecraftVersion)
        return cache.versions.find { it.first == MavenModule.KOTLIN_FOR_NEOFORGE }!!.second.version
    }

    private fun fetchLatestKotlinForNeoForgeVersion(minecraftVersion: String): String {
        val httpClient = HttpClient(CIO)

        val versions = runBlocking {
            json.decodeFromString<List<ModrinthVersion>>(httpClient.get("https://api.modrinth.com/v2/project/kotlin-for-forge/version") {
                parametersOf(mapOf(
                    "loaders" to listOf("neoforge"),
                    "game_versions" to listOf(minecraftVersion)
                ))
            }.bodyAsText())
        }

        val latest = versions.maxBy { Version.parse(it.versionNumber) }

        return latest.versionNumber
    }

    private fun updateCaches(minecraftVersion: String) {
        val cacheFile = File(CACHE_FILE)
        var cache = if (cacheFile.exists()) {
            val data = cacheFile.readText(Charsets.UTF_8)
            json.decodeFromString<VersionCache>(data)
        } else {
            val c = VersionCache.newest(minecraftVersion)
            cacheFile.writeText(json.encodeToString(c), Charsets.UTF_8)
            c
        }

        if (cache.minecraftVersion != minecraftVersion || cache.lastModified.until(Clock.System.now(), DateTimeUnit.MINUTE) > 5) {
            cache = VersionCache.newest(minecraftVersion)
            cacheFile.writeText(json.encodeToString(cache), Charsets.UTF_8)
        }

        this.cache = cache
    }

    @Serializable
    private data class MavenModule(
        val mavenUrl: String,
        val modulePackage: String,
        val module: String,
    ) {
        companion object {
            val NEOFORM = MavenModule("maven.neoforged.net", "net.neoforged", "neoform")
            val NEOFORGE = MavenModule("maven.neoforged.net", "net.neoforged", "neoforge")
            val FABRIC_LOADER = MavenModule("maven.fabricmc.net", "net.fabricmc", "fabric-loader")
            val FABRIC_API = MavenModule("maven.fabricmc.net", "net.fabricmc.fabric-api", "fabric-api")
            val FABRIC_LANG_KOTLIN = MavenModule("maven.fabricmc.net", "net.fabricmc", "fabric-language-kotlin")
            val KOTLIN_FOR_NEOFORGE = MavenModule("thedarkcolour.github.io/KotlinForForge", "thedarkcolour", "kotlinforforge-neoforge")
        }
    }

    @Serializable
    private data class CachedVersion(
        val version: String
    )

    @Serializable
    private data class VersionCache(
        val minecraftVersion: String,
        val lastModified: Instant,
        val versions: List<Pair<MavenModule, CachedVersion>> = listOf()
    ) {
        companion object {
            fun newest(minecraftVersion: String): VersionCache {
                val now = Clock.System.now()
                return VersionCache(
                    minecraftVersion, now,
                    listOf(
                        MavenModule.NEOFORM to CachedVersion(fetchLatestNeoformVersion(minecraftVersion)),
                        MavenModule.NEOFORGE to CachedVersion(fetchLatestNeoForgeVersion(minecraftVersion)),
                        MavenModule.FABRIC_LOADER to CachedVersion(fetchLatestFabricLoaderVersion()),
                        MavenModule.FABRIC_API to CachedVersion(fetchLatestFabricApiVersion(minecraftVersion)),
                        MavenModule.FABRIC_LANG_KOTLIN to CachedVersion(fetchLatestFabricLangKotlinVersion()),
                        MavenModule.KOTLIN_FOR_NEOFORGE to CachedVersion(fetchLatestKotlinForNeoForgeVersion(minecraftVersion))
                    )
                )
            }
        }
    }

    @Serializable
    private data class ModrinthVersion(
        val name: String,
        @SerialName("version_number") val versionNumber: String,
        val changelog: String,
        val dependencies: List<ModrinthDependency>,
        @SerialName("game_versions") val gameVersions: List<String>,
        @SerialName("version_type") val versionType: String,
        val loaders: List<String>,
        val featured: Boolean,
        val status: String,
        @SerialName("requested_status") val requestedStatus: String?,
        val id: String,
        @SerialName("project_id") val projectId: String,
        @SerialName("author_id") val authorId: String,
        @SerialName("date_published") val datePublished: String,
        val downloads: Int,
        @SerialName("changelog_url") val changelogUrl: String?,
        val files: List<ModrinthFile>,
    )

    @Serializable
    private data class ModrinthDependency(
        @SerialName("version_id") val versionId: String,
        @SerialName("project_id") val projectId: String,
        @SerialName("file_name") val fileName: String?,
        @SerialName("dependency_type") val dependencyType: String,
    )

    @Serializable
    private data class ModrinthFile(
        val hashes: Map<String, String>,
        val url: String,
        val filename: String,
        val primary: Boolean,
        val size: Int,
        @SerialName("file_type") val fileType: String?,
    )

    private data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
        companion object {
            fun parse(version: String): Version {
                val parts = version.split(".")
                val major = parts[0].toInt()
                val minor = parts[1].toInt()
                val patch = parts[2].toInt()

                return Version(major, minor, patch)
            }
        }

        override fun toString(): String {
            return "$major.$minor.$patch"
        }

        override fun compareTo(other: Version): Int {
            if (major > other.major) {
                return 1
            } else if (major < other.major) return -1

            if (minor > other.minor) {
                return 1
            } else if (minor < other.minor) return -1

            if (patch > other.patch) {
                return 1
            } else if (patch < other.patch) return -1

            return 0
        }
    }
}
