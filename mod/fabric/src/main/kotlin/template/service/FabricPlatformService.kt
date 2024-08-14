package template.service

import deplatformed.ServiceImpl
import net.fabricmc.loader.api.FabricLoader

@ServiceImpl([PlatformService::class])
class FabricPlatformService : PlatformService {
    override fun platform(): String = "fabric"
    override fun isModLoaded(modId: String): Boolean = FabricLoader.getInstance().isModLoaded(modId)
    override fun isDevelopmentEnvironment(): Boolean = FabricLoader.getInstance().isDevelopmentEnvironment
}