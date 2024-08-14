package template.service

import deplatformed.ServiceImpl
import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLEnvironment

@ServiceImpl([PlatformService::class])
class NeoForgePlatformService : PlatformService {
    override fun platform(): String = "neoforge"
    override fun isModLoaded(modId: String): Boolean = ModList.get().isLoaded(modId)
    override fun isDevelopmentEnvironment(): Boolean = !FMLEnvironment.production
}