package io.clappr.player.plugin

import io.clappr.player.base.BaseObject
import io.clappr.player.base.NamedType
import io.clappr.player.base.Options
import io.clappr.player.components.Playback
import io.clappr.player.components.PlaybackSupportInterface
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.primaryConstructor

class Loader(extraPlugins: List<KClass<out Plugin>> = emptyList(), extraPlaybacks: List<KClass<out Playback>> = emptyList()) {
    companion object {
        @JvmStatic private val registeredPlugins = mutableMapOf<String, KClass<out Plugin>>()
        @JvmStatic private val registeredPlaybacks= mutableListOf<KClass<out Playback>>()

        @JvmStatic
        fun clearPlaybacks() {
            registeredPlaybacks.clear()
        }

        @JvmStatic
        fun clearPlugins() {
            registeredPlugins.clear()
        }

        @JvmStatic
        fun registerPlugin(pluginClass: KClass<out Plugin>): Boolean {
            val pluginName = (pluginClass.companionObjectInstance as? NamedType)?.name
            pluginName?.let {
                if (pluginName.isNotEmpty()) {
                    registeredPlugins[pluginName] = pluginClass
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun unregisterPlugin(pluginClass: KClass<out Plugin>): Boolean {
            val pluginName = (pluginClass.companionObjectInstance as? NamedType)?.name
            pluginName?.let {
                if (it.isNotEmpty() && registeredPlugins.containsKey(it)) {
                    return registeredPlugins.remove(it) != null
                }
            }
            return false
        }

        @JvmStatic
        fun registerPlayback(playbackClass: KClass<out Playback>): Boolean {
            val playbackName = (playbackClass.companionObjectInstance as? NamedType)?.name
            playbackName?.let {
                if (playbackName.isNotEmpty()) {
                    registeredPlaybacks.removeAll { (it.companionObjectInstance as? NamedType)?.name == playbackName }
                    registeredPlaybacks.add(0, playbackClass)
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun supportsSource(playbackClass: KClass<out Playback>, source: String, mimeType: String? = null): Boolean {
            val companion = playbackClass.companionObjectInstance as? PlaybackSupportInterface
            companion?.let {
                return companion.supportsSource(source, mimeType)
            }
            return false
        }
    }

    private val externalPlugins = mutableListOf<KClass<out Plugin>>()

    private val externalPlaybacks = mutableListOf<KClass<out Playback>>()

    private val availablePlugins = mutableMapOf<String, KClass<out Plugin>>()

    private val availablePlaybacks = mutableListOf<KClass<out Playback>>()

    init {
        for (pluginClass in registeredPlugins.values) {
            addPlugin(pluginClass)
        }

        externalPlugins.addAll(extraPlugins.filter { !(it.companionObjectInstance as? NamedType)?.name.isNullOrEmpty() })
        for (pluginClass in externalPlugins) {
            addPlugin(pluginClass)
        }

        for (playbackClass in registeredPlaybacks) {
            addPlayback(playbackClass)
        }

        externalPlaybacks.addAll(extraPlaybacks.filter { !(it.companionObjectInstance as? NamedType)?.name.isNullOrEmpty() })
        for (playbackClass in externalPlaybacks) {
            addPlayback(playbackClass)
        }
    }

    fun loadPlugins(context: BaseObject) : List<Plugin> {
        val loadedPlugins = mutableListOf<Plugin>()
        availablePlugins.values.forEach {
            val plugin = loadPlugin(context, it)
            plugin?.let { loadedPlugins.add(plugin) }
        }
        return loadedPlugins.toList()
    }

    fun loadPlayback(source: String, mimeType: String? = null, options: Options): Playback? {
        var playback: Playback? = null

        try {
            val playbackClass = registeredPlaybacks.first { supportsSource(it, source, mimeType) }
            val constructor = playbackClass.primaryConstructor
            playback = constructor?.call(source, mimeType, options) as? Playback
        } catch (e: Exception) { }

        return playback
    }

    private fun addPlayback(playbackClass: KClass<out Playback>) {
        val name = (playbackClass.companionObjectInstance as? NamedType)?.name
        name?.let {
            if (!name.isEmpty()) {
                availablePlaybacks.add(playbackClass)
            }
        }
    }

    private fun addPlugin(pluginClass: KClass<out Plugin>) {
        val name : String? = (pluginClass.companionObjectInstance as? NamedType)?.name
        name?.let {
            if (name.isNotEmpty()) {
                availablePlugins[name] = pluginClass
            }
        }
    }

    private fun loadPlugin(component: BaseObject, pluginClass: KClass<out Plugin>) : Plugin? {
        var plugin: Plugin? = null
        val constructor = pluginClass.primaryConstructor

        try {
            plugin = constructor?.call(component) as? Plugin
        } catch (e: Exception) { }

        return plugin
    }
}