package de.michaelzinn.playerservices.data

import kotlinx.serialization.*

/**
 * Specifies which service was registered by a player.
 *
 * Should be deleted when the name no longer matches the UUID,
 * to force the player to re-register the service and to make
 * sure that the players using the service learn the new name,
 * to prevent name squatting. (If a player changes their
 * Minecraft name, that name gets blocked for a month)
 */
@Serializable
data class PlayerServiceEntry(
    val playerName: String,
    val playerUuid: String,
    val serviceUrl: String,
)