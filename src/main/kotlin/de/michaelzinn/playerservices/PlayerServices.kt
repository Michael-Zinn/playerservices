package de.michaelzinn.playerservices

import com.github.michaelbull.result.*
import de.michaelzinn.playerservices.async.AsyncDispatcher
import de.michaelzinn.playerservices.async.MainThreadDispatcher
import de.michaelzinn.playerservices.data.RegisteredService
import de.michaelzinn.playerservices.net.*
import de.michaelzinn.playerservices.util.sendErrorMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import java.net.MalformedURLException
import java.net.URL

@Suppress("unused") // Instantiated by the server
class PlayerServices : JavaPlugin() {
    private lateinit var delegate: PlayerServicesCommandExecutor

    override fun onLoad() {
        ConfigurationSerialization.registerClass(RegisteredService::class.java)
        saveDefaultConfig()
        val servicesConfigSection = config.getConfigurationSection("services") ?: config.createSection("services")
        saveConfig()

        delegate = PlayerServicesCommandExecutor(
            parentPlugin = this,
            playerServicesConfig = servicesConfigSection,
            client = PlayerServiceClient()
        )
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>?
    ): MutableList<String>? =
        delegate.onTabCompete(sender, command, args)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean =
        delegate.onCommand(sender, command, label, args)
}


class PlayerServicesCommandExecutor(
    private val parentPlugin: JavaPlugin,
    private val playerServicesConfig: ConfigurationSection,
    private val client: PlayerServiceClient,
    private val scheduler: BukkitScheduler = Bukkit.getScheduler(),
) : CommandExecutor {

    private val mainDispatcher = MainThreadDispatcher(parentPlugin, scheduler)
    private val asyncDispatcher = AsyncDispatcher(parentPlugin, scheduler)

    private suspend inline fun <T> async(crossinline code: () -> T) = withContext(asyncDispatcher) {
        code()
    }

    /*
    fun <T> async(
        runAsync: () -> T,
        callback: (T) -> Unit
    ) {
        scheduler.runTaskAsynchronously(parentPlugin, Runnable {
            val result = runAsync()
            scheduler.runTask(parentPlugin, Runnable {
                callback(result)
            })
        })
    }
     */

    fun onTabCompete(sender: CommandSender, command: Command, args: Array<out String>?): MutableList<String>? {
        if (sender !is Player) return null

        val tabCompletingSubcommandOrOwnerName = args?.size == 1
        if (!tabCompletingSubcommandOrOwnerName) return null

        val searchedOwnerName: String = args?.firstOrNull() ?: ""

        return when (command.name) {
            "ps" -> completeSubcommand(sender.name)
            "p", "s" -> completeServiceOwnerNames(searchedOwnerName).toMutableList()
            else -> null
        }
    }

    private fun completeSubcommand(senderName: String) =
        if (playerServicesConfig.contains(senderName))
            mutableListOf("register", "unregister")
        else
            mutableListOf("register")

    private fun completeServiceOwnerNames(searchedOwnerName: String) =
        playerServicesConfig.getKeys(false)
            .filter { it.startsWith(searchedOwnerName, ignoreCase = true) }
            .take(10)

    // Fake synchronous, returns true when it launches asynchronous stuff.
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        CoroutineScope(mainDispatcher).launch { onCommandAsync(sender, command, label, args) }
        return true
    }

    suspend fun onCommandAsync(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?,
    ): Boolean {
        parentPlugin.logger.info("Command $label (alias for ${command.name}) requested on ${parentPlugin.server.name}, ${parentPlugin.server.ip}, ${parentPlugin.server.port} by ${sender.name}")

        if (sender !is Player) {
            sender.sendPlainMessage("You must be a player to use PlayerServices commands")
            return false
        }

        return when (command.name) {
            "ps" -> handleRegistrationCommand(sender, command, args)
            "p" -> handleUserCommandPrivacyMode(sender, args)
            "s" -> handleUserCommandSharingMode(sender, args)
            else -> false
        }
    }

    private suspend fun handleRegistrationCommand(
        sender: Player,
        command: Command,
        args: Array<out String>?,
    ): Boolean =
        when {
            args.isNullOrEmpty() -> rejectEmptyCommand(sender)
            args.size == 1 && args[0] == "unregister" -> unregister(sender)
            args.size == 2 && args[0] == "register" -> register(sender, args[1])
            else -> false
        }

    private suspend fun handleUserCommandPrivacyMode(player: Player, args: Array<out String>?): Boolean {
        if (args.isNullOrEmpty()) return false

        val searchedServiceOwner = args[0]
        val service = searchServiceByOwner(searchedServiceOwner, player) ?: return false

        val providedArgs = args.drop(1)

        val result = async { client.privateRequest() }

        return result.fold(
            success = { response -> player.sendPlainMessage(response); true },
            failure = { err -> player.sendErrorMessage(err.message); false },
        )
    }

    private fun searchServiceByOwner(searchedServiceOwner: String, sender: Player): RegisteredService? {
        val exactMatch = playerServicesConfig.getObject(searchedServiceOwner, RegisteredService::class.java)
        if (exactMatch != null) return exactMatch

        val partialMatches = completeServiceOwnerNames(searchedServiceOwner)
        if (partialMatches.isEmpty()) {
            sender.sendErrorMessage("No service registered for player $searchedServiceOwner")
            return null
        }
        if (partialMatches.size > 1) {
            sender.sendErrorMessage("Player name $searchedServiceOwner is ambiguous, first ${partialMatches.size} candidates: ${partialMatches.joinToString()}")
            return null
        }

        return playerServicesConfig.getObject(partialMatches.first(), RegisteredService::class.java)!!
    }

    private suspend fun handleUserCommandSharingMode(player: Player, args: Array<out String>?): Boolean {
        if (args.isNullOrEmpty()) return false

        val searchedServiceOwner = args[0]
        val service = searchServiceByOwner(searchedServiceOwner, player) ?: return false

        val serviceArgs = args.drop(1)

        val requestBody = PlayerServiceRequestBody(
            server = ServerInfo(
                mcServerName = parentPlugin.server.name,
                mcServerIp = parentPlugin.server.ip,
            ),
            player = PlayerInfo(
                name = player.name,
                // displayName = player.displayName().toString(),
                uuid = player.uniqueId.toString(),
                location = PlayerLocationInfo(
                    worldName = player.world.name,

                    x = player.x,
                    y = player.y,
                    z = player.z,

                    pitch = player.pitch,
                    yaw = player.yaw,
                )
            ),
            message = serviceArgs.joinToString(" ") ?: ""
        )

        val result = async { client.sharingRequest(service.url, requestBody) }

        return result.fold(
            success = { response -> player.sendPlainMessage(response); true },
            failure = { err -> player.sendErrorMessage(err.message); false },
        )
    }

    private fun rejectEmptyCommand(sender: Player): Boolean {
        sender.sendErrorMessage("No subcommand given")
        return false
    }

    private fun unregister(sender: Player): Boolean {
        fun Player.sendUnregistrationMessage() = this.sendRichMessage("<green>Service unregistered for</green> $name")

        if (!playerServicesConfig.contains(sender.name)) return false
        if (hasDifferentPlayerUuid(sender)) return false

        playerServicesConfig[sender.name] = null
        parentPlugin.saveConfig()
        sender.sendUnregistrationMessage()
        return true
    }

    private suspend fun register(player: Player, serviceUrl: String): Boolean {
        fun Player.sendRegistrationMessage(playerServiceUrl: URL) =
            this.sendRichMessage("<green>Service registered for</green> $name <green>at</green> $playerServiceUrl")

        try {
            val url = URL(serviceUrl)

            // Can't register if you squatted the player name from someone else.
            // This could happen if the original player changed their username.
            if (hasDifferentPlayerUuid(player)) return false

            val requestBody = PlayerServiceRegistrationRequestBody(
                mcServerName = player.server.name,
                mcServerIp = player.server.ip,
                playerName = player.name,
                playerUuid = player.uniqueId.toString(),
                serviceUrl = serviceUrl,
            )

            val result = async { client.register(requestBody) }

            return result.fold(
                success = {
                    val newService = RegisteredService(player.uniqueId, url)
                    playerServicesConfig[player.name] = newService
                    parentPlugin.saveConfig()
                    player.sendRegistrationMessage(newService.url)
                    true
                },
                failure = { err ->
                    player.sendErrorMessage(err.message)
                    false
                }
            )
        } catch (ex: MalformedURLException) {
            player.sendErrorMessage("Invalid URL: $serviceUrl")
            return false
        }
    }

    private fun getRegisteredService(name: String): RegisteredService? {
        return playerServicesConfig.getObject(name, RegisteredService::class.java)
    }

    private fun hasDifferentPlayerUuid(sender: Player): Boolean {
        val currentOwnerId = getRegisteredService(sender.name)?.ownerId
        val hasPlayerUuidChanged = currentOwnerId?.let { it != sender.uniqueId }
        return hasPlayerUuidChanged ?: false
    }
}
