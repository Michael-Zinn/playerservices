package de.michaelzinn.playerservices.persistence

import de.michaelzinn.playerservices.data.PlayerServiceEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.plugin.Plugin
import java.io.File

class PlayerServiceRegistryPersistence(
    plugin: Plugin,
) {
    companion object {
        const val filename = "PlayerServices.json"
    }

    private val file = File(plugin.dataFolder, filename)

    private var playerServices: MutableMap<String, PlayerServiceEntry> =
        load().associateBy { it.playerName }.toMutableMap()

    private fun load(): List<PlayerServiceEntry> {
        return if (file.exists()) {
            val jsonStringFromFile = file.readText()
            Json.decodeFromString<List<PlayerServiceEntry>>(jsonStringFromFile)
        } else {
            emptyList()
        }
    }

    fun get() = playerServices

    private fun save() {
        val json = Json.encodeToString(playerServices.values.sortedBy { it.playerName })
        // Write JSON data to the file
        file.writeText(json)
    }

    fun add(playerService: PlayerServiceEntry) {
        this.playerServices.put(playerService.playerName, playerService)
        save()
    }

    fun remove(playerName: String) {
        playerServices.remove(playerName)
        save()
    }
}