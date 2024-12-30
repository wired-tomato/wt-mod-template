package template.service

interface PlatformService {
    fun platform(): String
    fun isModLoaded(modId: String): Boolean
    fun isDevelopmentEnvironment(): Boolean
    fun getEnvironmentName(): String = if (isDevelopmentEnvironment()) "development" else "production"
}

val PlatformServiceImpl = Services.getService<PlatformService>()
