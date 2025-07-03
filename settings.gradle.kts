import java.nio.file.Files
import java.util.*
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

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

        maven("https://maven.minecraftforge.net/") {
            name = "MinecraftForge"
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

val minecraft_version: String by extra
val minecraftVersion = Version(minecraft_version)

val platforms = listOf("fabric", "forge", "neoforge")

include("deplatformed-api", "deplatformed-ksp")
project(":deplatformed-api").projectDir = file("deplatformed/api")
project(":deplatformed-ksp").projectDir = file("deplatformed/ksp")

val enabledVersions = platforms.associateWith { parseVersionRange(it) }

include("common")
project(":common").projectDir = file("mod/common")

if (enabledVersions["fabric"]!!.isVersionInRange(minecraftVersion)) {
    include("fabric")
    project(":fabric").projectDir = file("mod/fabric")
} else println("Fabric has been disabled as $minecraftVersion does not fit in range ${enabledVersions["fabric"]!!}")

if (enabledVersions["forge"]!!.isVersionInRange(minecraftVersion)) {
    include("forge")
    project(":forge").projectDir = file("mod/forge")
} else println("Forge has been disabled as $minecraftVersion does not fit in range ${enabledVersions["forge"]!!}")

if (enabledVersions["neoforge"]!!.isVersionInRange(minecraftVersion)) {
    include("neoforge")
    project(":neoforge").projectDir = file("mod/neoforge")
} else println("Neoforge has been disabled as $minecraftVersion does not fit in range ${enabledVersions["neoforge"]!!}")

rootProject.name = "wt-mod-template"

fun parseVersionRange(platform: String): VersionRange {
    return VersionRange.parse(runCatching { Files.readString(rootDir.toPath().resolve("mod/${platform}/enabled_versions")) }.getOrElse { "" })
}

class Version(
    val versionString: String,
) : Comparable<Version> {

    override operator fun compareTo(other: Version): Int {
        return FlexVerComparator.compare(versionString, other.versionString)
    }
    override fun toString(): String {
        return versionString
    }
}

class VersionRange(
    val beginInclusive: Boolean,
    val endInclusive: Boolean,
    val begin: Version,
    val end: Version
) {

    fun isVersionInRange(version: Version): Boolean {
        val effectiveEnd = if (end.versionString.isBlank()) null else end

        return when {
            beginInclusive && endInclusive -> version in begin..(effectiveEnd ?: version)
            beginInclusive -> version >= begin && (effectiveEnd?.let { version < it } ?: true)
            endInclusive -> version > begin && version <= (effectiveEnd ?: version)
            else -> version > begin && (effectiveEnd?.let { version < it } ?: true)
        }
    }

    override fun toString(): String {
        return "${if (beginInclusive) "[" else "("}$begin,$end${if (endInclusive) "]" else ")"}"
    }

    companion object {
        val PATTERN = Pattern.compile("([(\\[])(.*),(.*)([)\\]])")

        val ALWAYS_TRUE = VersionRange(beginInclusive = true, endInclusive = false, begin = Version(
            "-1"
        ), end = Version("")
        )
        fun parse(range: String): VersionRange {
            if (range.isEmpty()) return ALWAYS_TRUE

            val matcher = PATTERN.matcher(range)
            matcher.find()

            val beginInclusive = matcher.group(1) == "["
            val end = Version(matcher.group(3))
            val endInclusive = if (end.versionString.isBlank()) {
                false
            } else {
                matcher.group(4) == "]"
            }

            val begin = Version(matcher.group(2))

            return VersionRange(beginInclusive, endInclusive, begin, end)
        }
    }
}

/**
 * Implements FlexVer, a SemVer-compatible intuitive comparator for free-form versioning strings as
 * seen in the wild. It's designed to sort versions like people do, rather than attempting to force
 * conformance to a rigid and limited standard. As such, it imposes no restrictions. Comparing two
 * versions with differing formats will likely produce nonsensical results (garbage in, garbage out),
 * but best effort is made to correct for basic structural changes, and versions of differing length
 * will be parsed in a logical fashion.
 *
 * changes - converted to kotlin
 */
object FlexVerComparator {
    /**
     * Parse the given strings as freeform version strings, and compare them according to FlexVer.
     * @param a the first version string
     * @param b the second version string
     * @return `0` if the two versions are equal, a negative number if `a < b`, or a positive number if `a > b`
     */
    fun compare(a: String, b: String): Int {
        val ad = decompose(a)
        val bd = decompose(b)
        for (i in 0 until max(ad.size, bd.size)) {
            val c = get(ad, i).compareTo(get(bd, i))
            if (c != 0) return c
        }
        return 0
    }


    private val NULL: VersionComponent = object : VersionComponent(IntArray(0)) {
        override fun compareTo(that: VersionComponent): Int {
            return if (that === this) 0 else -that.compareTo(this)
        }
    }

    /*
	 * Break apart a string into intuitive version components, by splitting it where a run of
	 * characters changes from numeric to non-numeric.
	 */
    // @VisibleForTesting
    fun decompose(str: String): List<VersionComponent> {
        if (str.isEmpty()) return emptyList()
        var lastWasNumber = isAsciiDigit(str.codePointAt(0))
        val totalCodepoints = str.codePointCount(0, str.length)
        val accum = IntArray(totalCodepoints)
        val out: MutableList<VersionComponent> = ArrayList()
        var j = 0
        var i = 0
        while (i < str.length) {
            val cp = str.codePointAt(i)
            if (Character.charCount(cp) == 2) i++
            if (cp == '+'.code) break // remove appendices

            val number = isAsciiDigit(cp)
            if (number != lastWasNumber || (cp == '-'.code && j > 0 && accum[0] != '-'.code)) {
                out.add(createComponent(lastWasNumber, accum, j))
                j = 0
                lastWasNumber = number
            }
            accum[j] = cp
            j++
            i++
        }
        out.add(createComponent(lastWasNumber, accum, j))
        return out
    }

    private fun isAsciiDigit(cp: Int): Boolean {
        return cp >= '0'.code && cp <= '9'.code
    }

    private fun createComponent(number: Boolean, s: IntArray, j: Int): VersionComponent {
        var s = s
        s = Arrays.copyOfRange(s, 0, j)
        return if (number) {
            NumericVersionComponent(s)
        } else if (s.size > 1 && s[0] == '-'.code) {
            SemVerPrereleaseVersionComponent(s)
        } else {
            VersionComponent(s)
        }
    }

    private fun get(li: List<VersionComponent>, i: Int): VersionComponent {
        return if (i >= li.size) NULL else li[i]
    }

    // @VisibleForTesting
    open class VersionComponent(private val codepoints: IntArray) {
        fun codepoints(): IntArray {
            return codepoints
        }

        open fun compareTo(that: VersionComponent): Int {
            if (that === NULL) return 1
            val a = this.codepoints()
            val b = that.codepoints()

            for (i in 0 until min(a.size, b.size)) {
                val c1 = a[i]
                val c2 = b[i]
                if (c1 != c2) return c1 - c2
            }

            return a.size - b.size
        }

        override fun toString(): String {
            return String(codepoints, 0, codepoints.size)
        }
    }

    // @VisibleForTesting
    internal class SemVerPrereleaseVersionComponent(codepoints: IntArray) : VersionComponent(codepoints) {
        override fun compareTo(that: VersionComponent): Int {
            if (that === NULL) return -1 // opposite order

            return super.compareTo(that)
        }
    }

    // @VisibleForTesting
    internal class NumericVersionComponent(codepoints: IntArray) : VersionComponent(codepoints) {
        override fun compareTo(that: VersionComponent): Int {
            if (that === NULL) return 1
            if (that is NumericVersionComponent) {
                val a = removeLeadingZeroes(this.codepoints())
                val b = removeLeadingZeroes(that.codepoints())
                if (a.size != b.size) return a.size - b.size
                for (i in a.indices) {
                    val ad = a[i]
                    val bd = b[i]
                    if (ad != bd) return ad - bd
                }
                return 0
            }
            return super.compareTo(that)
        }

        private fun removeLeadingZeroes(a: IntArray): IntArray {
            if (a.size == 1) return a
            var i = 0
            val stopIdx = a.size - 1
            while (i < stopIdx && a[i] == '0'.code) {
                i++
            }
            return Arrays.copyOfRange(a, i, a.size)
        }
    }
}
