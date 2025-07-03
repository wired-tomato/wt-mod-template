package net.wiredtomato.versioning

import java.util.regex.Pattern

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
        val ALWAYS_TRUE = VersionRange(beginInclusive = true, endInclusive = false, begin = Version("-1"), end = Version(""))

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
