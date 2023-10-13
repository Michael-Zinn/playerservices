package de.michaelzinn.playerservices

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.net.MalformedURLException
import java.net.URL
import java.util.*

@Suppress("unused") // Instantiated by the server
class PlayerServices : JavaPlugin() {
    private lateinit var delegate: PlayerServicesCommandExecutor

    override fun onLoad() {
        ConfigurationSerialization.registerClass(RegisteredService::class.java)
        saveDefaultConfig()
        val servicesConfigSection = config.getConfigurationSection("services") ?: config.createSection("services")
        saveConfig()

        delegate = PlayerServicesCommandExecutor(servicesConfigSection, this)
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
    private val playerServicesConfig: ConfigurationSection,
    private val parentPlugin: JavaPlugin
) : CommandExecutor {
    fun onTabCompete(sender: CommandSender, command: Command, args: Array<out String>?): MutableList<String>? {
        if (sender !is Player) return null

        val tabCompletingSubcommandOrPlayerName = args?.size == 1
        if (!tabCompletingSubcommandOrPlayerName) return null

        return when (command.name) {
            "ps" -> mutableListOf(if (playerServicesConfig.contains(sender.name)) "unregister" else "register")
            else -> null
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        parentPlugin.logger.info("Command $label (alias for ${command.name}) requested on ${parentPlugin.server.name}, ${parentPlugin.server.ip}, ${parentPlugin.server.port} by ${sender.name}")

        if (sender !is Player) {
            sender.sendPlainMessage("You must be a player to use PlayerServices commands")
            return false
        }

        return when (command.name) {
            "ps" -> handleAdminCommand(sender, command, args)
            "p" -> handleUserCommandPrivacyMode()
            "s" -> handleUserCommandSharingMode()
            else -> false
        }
    }

    private fun handleAdminCommand(sender: Player, command: Command, args: Array<out String>?): Boolean =
        when {
            args.isNullOrEmpty() -> rejectEmptyCommand(sender)
            args.size == 1 && args[0] == "unregister" -> unregister(sender)
            args.size == 2 && args[0] == "register" -> register(sender, args[1])
            else -> false
        }

    private fun handleUserCommandPrivacyMode(): Boolean = false
    private fun handleUserCommandSharingMode(): Boolean = false

    private fun rejectEmptyCommand(sender: Player): Boolean {
        sender.sendErrorMessage("No subcommand given")
        return false
    }

    private fun unregister(sender: Player): Boolean {
        if (!playerServicesConfig.contains(sender.name)) return false
        if (hasDifferentPlayerUuid(sender)) return false

        playerServicesConfig[sender.name] = null
        parentPlugin.saveConfig()
        sender.sendUnregistrationMessage()
        return true
    }

    private fun register(sender: Player, serviceUrl: String): Boolean {
        try {
            if (hasDifferentPlayerUuid(sender)) return false

            val newService = RegisteredService(sender.uniqueId, URL(serviceUrl))
            playerServicesConfig[sender.name] = newService
            parentPlugin.saveConfig()
            sender.sendRegistrationMessage(newService.url)
            return true
        } catch (ex: MalformedURLException) {
            sender.sendErrorMessage("Invalid URL: $serviceUrl")
            return false
        }
    }

    private fun hasDifferentPlayerUuid(sender: Player): Boolean {
        val currentOwnerId = playerServicesConfig.getObject(sender.name, RegisteredService::class.java)?.ownerId
        val hasPlayerUuidChanged = currentOwnerId?.let { it != sender.uniqueId }
        return hasPlayerUuidChanged ?: false
    }
}

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

fun Player.sendRegistrationMessage(playerServiceUrl: URL) =
    this.sendRichMessage("<green>Service registered for</green> $name <green>at</green> $playerServiceUrl")

fun Player.sendUnregistrationMessage() = this.sendRichMessage("<green>Service unregistered for</green> $name")

fun Player.sendErrorMessage(message: String) = this.sendRichMessage("<red>Error:</red> $message")