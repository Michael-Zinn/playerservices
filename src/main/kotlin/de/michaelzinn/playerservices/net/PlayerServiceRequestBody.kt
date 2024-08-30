package de.michaelzinn.playerservices.net

import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    val mcServerName: String,
    val mcServerIp: String,
)

@Serializable
data class PlayerLocationInfo(

    // Can be server specific. Default names are: world, world_nether, world_the_end
    val worldName: String,

    // player coordinates (center of the player's bottom side)
    val x: Double,
    val y: Double,
    val z: Double,

    // block coordinates are left out, since they are always the player coordinates rounded towards negative infinity.

    // player head rotation
    val pitch: Float, //  -90 (up) to 90 (down)
    // roll can not be changed.
    val yaw: Float, //  180 (north), -179.9 ... -90 (east) ... -0.0 (south), 0.1 ... 90 (west) ... 180
)

@Serializable
data class PlayerInfo(
    val name: String,
    // displayName: String, TODO complex, not really a String, only used on some servers.
    val uuid: String,
    val location: PlayerLocationInfo,
    // item in hand?
)

@Serializable
data class PlayerServiceRequestBody(
    val server: ServerInfo,
    val player: PlayerInfo,
    val message: String,
)
