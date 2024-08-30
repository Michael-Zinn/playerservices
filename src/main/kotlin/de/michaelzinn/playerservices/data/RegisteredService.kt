package de.michaelzinn.playerservices.data

import org.bukkit.configuration.serialization.ConfigurationSerializable
import java.net.URL
import java.util.*

data class RegisteredService(val ownerId: UUID, val url: URL) : ConfigurationSerializable {
    override fun serialize() = mutableMapOf(
        "ownerId" to ownerId.toString(),
        "url" to url.toString()
    )

    override fun toString() = "RegisteredService(ownerId=$ownerId, url=$url)"

    companion object {
        @JvmStatic
        @Suppress("unused") // Called by the server for deserialization
        fun deserialize(args: Map<String, Any>) = RegisteredService(
            UUID.fromString(args["ownerId"] as String),
            URL(args["url"] as String)
        )
    }
}