package de.michaelzinn.playerservices.data

import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    val mcServerName: String,
    val mcServerIp: String,
)

@Serializable
data class PlayerLocationInfo(
    val worldName: String, // usually world, world_nether ?

    val x: Double,
    val y: Double,
    val z: Double,

    //val blockX: Int,
    //val blockY: Int,
    //val blockZ: Int,

    val pitch: Float, //  -90 (up) to 90 (down)
    val yaw: Float, //  180 (north), -179.9 ... -90 (east) ... -0.0 (south), 0.1 ... 90 (west) ... 180
)

@Serializable
data class PlayerInfo(
    val name: String,
    // val displayName: String, TODO complex, not really a String
    val uuid: String,
    val location: PlayerLocationInfo,
    // book in hand?
)

@Serializable
data class PlayerServiceRequestBody(
    val server: ServerInfo,
    val player: PlayerInfo,
    val message: String,
)
