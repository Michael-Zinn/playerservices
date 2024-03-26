package de.michaelzinn.playerservices

import de.michaelzinn.playerservices.data.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class PlayerServices : JavaPlugin() {

    private val playerServices: MutableMap<String, PlayerServiceEntry> by lazy {
        Persistence.load().associateBy { it.playerName }.toMutableMap()
    }

    val client = OkHttpClient()
    val playerServiceMediaType = "application/prs.playerservices-mc.v0+json".toMediaType()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val player = sender as? Player
        if (player == null) {
            sender.sendPlainMessage("Currently, commands can only be issued from players.")
            return true
        }

        if (args.isEmpty()) {
            player.sendPlainMessage("You need to specify a sub-command: help, ls, register")
            return true
        }
        val subCommand = args.first()
        val subArgs = args.drop(1)
        val message: String = subArgs.joinToString(" ")

        logger.info("Command $label (alias for ${command.name}) requested on ${this.server.name}, ${this.server.ip}, ${this.server.port} by ${sender.name}")
        //  player.sendPlainMessage("Hallo")
        // player.sendPlainMessage("Gerade existierende Services: $playerServices")

        return when (command.name) {
            "ps" -> handleConfigCommand(player, subCommand, subArgs)
            "s" -> handleUserCommand(player, subCommand, message)
            else -> false
        }
    }

    private fun handleConfigCommand(sender: CommandSender, subCommand: String, subArgs: List<String>): Boolean {
        val player = (sender as Player)
        val playerUuid = player.identity().uuid().toString()

        //sender.sendPlainMessage("You used the ps command. Good job! $playerUuid")
        //sender.sendPlainMessage("Args were: $subArgs")

        when (subCommand) {
            "help" -> {
                sender.sendPlainMessage("Help goes here. SubCommands: help, ls, register.")
            }

            "debugls" -> {
                val serviceList = playerServices.values.joinToString("\n") { entry ->
                    "${entry.playerName}(${entry.playerUuid}) -> ${entry.serviceUrl}"
                }
                sender.sendPlainMessage("Services:\n$serviceList")
            }

            "ls" -> {
                val serviceList = playerServices.values.joinToString(",") { entry ->
                    entry.playerName
                }
                sender.sendPlainMessage("Services:\n    $serviceList")
            }

            "register" -> {
                if (subArgs.size != 1) {
                    sender.sendPlainMessage("Register takes exactly one argument!")
                    return true
                }
                val serviceUrl = subArgs.first()

                sender.sendPlainMessage("Attempting to register $serviceUrl for ${player.name} ...")

                val requestBody = Json.encodeToString(
                    PlayerServiceRegistrationRequestBody(
                        mcServerName = sender.server.name,
                        mcServerIp = sender.server.ip,
                        playerName = player.name,
                        playerUuid = playerUuid,
                        serviceUrl = serviceUrl,
                    )
                ).toRequestBody(playerServiceMediaType)

                val request = Request.Builder()
                    .url("$serviceUrl/registration")
                    .put(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        player.sendPlainMessage("Registration was not successful.\nCode: ${response.code}\nResponse:\n${response}")
                        return true
                    }
                    if (response.code != 201) {
                        player.sendPlainMessage("""
                            Protocol requires response code 201. Was ${response.code}.
                            Response:
                            ${response}
                            
                            Body:
                            ${response.body?.string()}
                            
                            Message:
                            ${response.message}
                            
                            Headers:
                            ${response.headers}
                        """.trimIndent())
                        return true
                    }

                    val entry = PlayerServiceEntry(
                        playerName = player.name,
                        playerUuid = playerUuid,
                        serviceUrl = serviceUrl,
                    )

                    playerServices[sender.name] = entry
                    Persistence.save(playerServices.values.toList())

                    sender.sendPlainMessage("Registered!")
                }
            }

            "unregister" -> {
                if (playerServices[sender.name] != null) {
                    playerServices.remove(sender.name)
                    Persistence.save(playerServices.values.toList())
                    sender.sendPlainMessage("Service unregistered!")
                } else {
                    sender.sendPlainMessage("You don't have a service registered!")
                }
            }

            else -> {
                sender.sendPlainMessage("Command `$subCommand` not found, please use help, ls or register.")
            }
        }

        return true
    }

    private fun resolve(playerServiceName: String): List<String> {
        val serviceNames = playerServices.keys.sorted()

        return serviceNames.filter { name ->
            name.startsWith(playerServiceName, ignoreCase = true)
        }
    }

    private fun handleUserCommand(player: Player, playerServiceName: String, message: String): Boolean {
        val matches = resolve(playerServiceName)

        when {
            matches.isEmpty() -> {
                player.sendPlainMessage("Could not find a service for $playerServiceName")
                return true
            }
            matches.size >= 2 -> {
                player.sendPlainMessage(
                    "Name $playerServiceName is ambiguous: ${matches.joinToString(separator = ", ")}"
                )
                return true
            }
        }

        val match = matches.first()

        val url = playerServices[match]!!.serviceUrl
        /*
        with(player) {
            sendPlainMessage(
                """
                name: $name
                yaw: $yaw
                pitch: $pitch
                bodyyaw: $bodyYaw
                x: $x
                y: $y
                z: $z
                item on cursor: $itemOnCursor
                location world name: ${location.world.name}
            """.trimIndent()
            )
        }
*/
        val requestBody = Json.encodeToString(
            PlayerServiceRequestBody(
                server = ServerInfo(
                    mcServerName = this.server.name,
                    mcServerIp = this.server.ip,
                ),
                player = PlayerInfo(
                    name = player.name,
                    // displayName = player.displayName().toString(),
                    uuid = player.identity().uuid().toString(),
                    location = PlayerLocationInfo(
                        worldName = player.world.name, // usually world, world_nether ?

                        x = player.x,
                        y = player.y,
                        z = player.z,

                        //blockX = player.location.blockX,
                        //blockY = player.location.blockY,
                        //blockZ = player.location.blockZ,

                        pitch = player.pitch,
                        yaw = player.yaw,
                    )
                ),
                message = message
            )
        ).toRequestBody(playerServiceMediaType)

        val request = Request.Builder()
            .url("$url/command")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")

            // Handle the response
            player.sendPlainMessage(response.body!!.string())
        }

        return true
    }
}
