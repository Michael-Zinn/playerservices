package de.michaelzinn.playerservices

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import de.michaelzinn.playerservices.async.AsyncDispatcher
import de.michaelzinn.playerservices.async.MainThreadDispatcher
import de.michaelzinn.playerservices.net.*
import de.michaelzinn.playerservices.persistence.PlayerServiceRegistry
import de.michaelzinn.playerservices.persistence.PlayerServiceRegistryPersistence
import de.michaelzinn.playerservices.util.Ok
import de.michaelzinn.playerservices.util.sendErrorMessage
import de.michaelzinn.playerservices.util.toBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import java.net.URL

@Suppress("unused") // Instantiated by the server
class PlayerServices : JavaPlugin() {
    private lateinit var delegate: PlayerServicesCommandExecutor

    override fun onLoad() {
        saveDefaultConfig()
        saveConfig()

        delegate = PlayerServicesCommandExecutor(
            parentPlugin = this,
            registry = PlayerServiceRegistry(PlayerServiceRegistryPersistence(this)),
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
    private val registry: PlayerServiceRegistry,
    private val client: PlayerServiceClient,
    scheduler: BukkitScheduler = Bukkit.getScheduler(),
) : CommandExecutor {


    private val mainDispatcher = MainThreadDispatcher(parentPlugin, scheduler)
    private val asyncDispatcher = AsyncDispatcher(parentPlugin, scheduler)

    private suspend inline fun <T> async(crossinline code: () -> T) = withContext(asyncDispatcher) {
        code()
    }

    fun onTabCompete(sender: CommandSender, command: Command, args: Array<out String>?): MutableList<String>? {
        if (sender !is Player) return null

        val tabCompletingSubcommandOrOwnerName = args?.size == 1
        if (!tabCompletingSubcommandOrOwnerName) return null

        val searchedOwnerName: String = args?.firstOrNull() ?: ""

        return when (command.name) {
            "ps" -> {
                val bla = completeSubcommand(sender.name)
                bla
            }

            "p", "s" -> {
                val foundNames = registry.completeServiceOwnerNames(searchedOwnerName).toMutableList()
                foundNames
            }

            else -> null
        }
    }

    private fun completeSubcommand(senderName: String) =
        if (registry.contains(senderName)) {
            mutableListOf("register", "unregister")
        } else {
            mutableListOf("register")
        }

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
            "ps" -> handleRegistrationCommand(sender, args)
            "p" -> handleUserCommandPrivacyMode(sender, args)
            "s" -> handleUserCommandSharingMode(sender, args)
            else -> false
        }
    }

    private suspend fun handleRegistrationCommand(
        player: Player,
        args: Array<out String>?,
    ): Boolean =
        when {
            args.isNullOrEmpty() -> rejectEmptyCommand(player)
            args.size == 1 && args[0] == "unregister" -> unregister(player)
            args.size == 2 && args[0] == "register" -> register(player, args[1])
            else -> false
        }

    private suspend fun handleUserCommandPrivacyMode(
        player: Player,
        args: Array<out String>?
    ): Boolean = coroutineBinding {
        if (args.isNullOrEmpty()) Err("No player name given").bind<Unit>()
        val searchedServiceOwner = args!![0]
        val service = registry.searchServiceByOwner(searchedServiceOwner, player).bind()

        val providedArgs = args.drop(1)

        val response = async { client.privateRequest() }.bind()

        player.sendPlainMessage(response)
        Ok().bind()
    }.mapError { err: String ->
        player.sendErrorMessage(err)
    }.toBoolean()

    private suspend fun handleUserCommandSharingMode(
        player: Player,
        args: Array<out String>?
    ): Boolean = coroutineBinding {
        if (args.isNullOrEmpty()) Err("No player name given").bind<String>()

        val searchedServiceOwner = args!![0]
        val service = registry.searchServiceByOwner(searchedServiceOwner, player).bind()

        val serviceArgs = args.drop(1)

        val requestBody = PlayerServiceRequestBody(
            server = ServerInfo(
                mcServerName = parentPlugin.server.name,
                mcServerIp = parentPlugin.server.ip,
            ),
            player = PlayerInfo(
                name = player.name,
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
            message = serviceArgs.joinToString(" ")
        )

        val response = async { client.sharingRequest(service.serviceUrl, requestBody) }.bind()
        player.sendPlainMessage(response)

    }.mapError { err: String ->
        player.sendErrorMessage(err)
    }.toBoolean()

    private fun rejectEmptyCommand(sender: Player): Boolean {
        sender.sendErrorMessage("No subcommand given")
        return false
    }

    private fun unregister(player: Player): Boolean {
        fun Player.sendUnregistrationMessage() = this.sendRichMessage("<green>Service unregistered for</green> $name")

        val success = registry.unregister(player)

        if (success) player.sendUnregistrationMessage()
        return success
    }

    private suspend fun register(
        player: Player,
        serviceUrl: String
    ): Boolean = coroutineBinding {

        fun Player.sendRegistrationMessage(playerServiceUrl: URL) = this.sendRichMessage(
            "<green>Service registered for</green> $name <green>at</green> $playerServiceUrl"
        )

        // throws MalformedURLException
        val url = runCatching { URL(serviceUrl) }.mapError { "Invalid URL: $serviceUrl" }.bind()

        // Can't register if you squatted the player name from someone else.
        // This could happen if the original player changed their username.
        if (registry.hasDifferentPlayerUuid(player)) Err("Contact your server admin").bind<Unit>()

        val requestBody = PlayerServiceRegistrationRequestBody(
            mcServerName = player.server.name,
            mcServerIp = player.server.ip,
            playerName = player.name,
            playerUuid = player.uniqueId.toString(),
            serviceUrl = serviceUrl,
        )

        async { client.register(requestBody) }.mapError { it.message }.bind()

        registry.register(
            playerName = player.name,
            playerUuid = player.uniqueId,
            serviceUrl = url,
        )
        player.sendRegistrationMessage(url)

    }.mapError { err: String ->
        player.sendErrorMessage(err)
    }.toBoolean()


}
