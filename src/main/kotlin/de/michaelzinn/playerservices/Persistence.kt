package de.michaelzinn.playerservices

import de.michaelzinn.playerservices.data.PlayerServiceEntry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

data object Persistence {
    const val filename = "plugins/PlayerServices.json"

    fun save(playerServiceEntries: List<PlayerServiceEntry>) {
        val json = Json.encodeToString(playerServiceEntries)

        // Write JSON data to the file
        val file = File(filename)
        file.writeText(json)
    }

    fun load(): List<PlayerServiceEntry> {
        val file = File(filename)

        return if (file.exists()) {
            val jsonStringFromFile = file.readText()
            Json.decodeFromString<List<PlayerServiceEntry>>(jsonStringFromFile)
        } else {
            emptyList()
        }
    }

}