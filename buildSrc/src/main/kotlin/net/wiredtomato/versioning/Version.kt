package net.wiredtomato.versioning

import com.unascribed.flexver.FlexVerComparator

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
