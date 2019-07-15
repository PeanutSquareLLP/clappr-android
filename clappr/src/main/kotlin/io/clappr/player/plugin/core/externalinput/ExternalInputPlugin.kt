package io.clappr.player.plugin.core.externalinput

import io.clappr.player.base.NamedType
import io.clappr.player.components.Core
import io.clappr.player.plugin.PluginEntry
import io.clappr.player.plugin.core.CorePlugin

class ExternalInputPlugin(core: Core) : CorePlugin(core, name = name) {
    companion object : NamedType {
        override val name = "externalInputPlugin"

        val entry = PluginEntry.Core(name = name, factory = { core -> ExternalInputPlugin(core) })
    }
}