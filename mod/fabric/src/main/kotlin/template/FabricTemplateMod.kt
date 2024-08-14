package template

import net.fabricmc.api.ModInitializer

object FabricTemplateMod : ModInitializer {
    override fun onInitialize() {
        TemplateMod.commonInit()
    }
}