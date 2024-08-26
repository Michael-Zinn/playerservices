package de.michaelzinn.playerservices.net

import kotlinx.serialization.Serializable

/**
 * Gets send to a player service when a player tries to register it.
 */
@Serializable
data class PlayerServiceRegistrationRequestBody(
    val mcServerName: String,
    val mcServerIp: String,
    val playerName: String,
    val playerUuid: String,
    val serviceUrl: String,
)