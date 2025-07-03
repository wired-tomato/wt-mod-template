object CommonVersionRetrievers {
    private val retrievers = mutableMapOf<String, VersionRetriever>()

    fun getOrCreate(minecraftVersion: String): VersionRetriever {
        return retrievers.computeIfAbsent(minecraftVersion) { VersionRetriever(minecraftVersion) }
    }
}