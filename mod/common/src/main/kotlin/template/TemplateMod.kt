package template

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import template.service.PlatformServiceImpl

object TemplateMod {
    const val MOD_ID = "template"
    @JvmStatic
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    fun commonInit() {
        LOGGER.info("Loading $MOD_ID for ${PlatformServiceImpl.platform()}")
    }
}