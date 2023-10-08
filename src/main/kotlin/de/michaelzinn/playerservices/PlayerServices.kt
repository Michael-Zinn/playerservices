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

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        return delegate.onCommand(sender, command, label, args)
    }
}


class PlayerServicesCommandExecutor(
    private val playerServicesConfig: ConfigurationSection,
    private val parentPlugin: JavaPlugin
) : CommandExecutor {
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


    private fun handleAdminCommand(sender: Player, command: Command, args: Array<out String>?): Boolean {
        if (args == null || args.isEmpty())
            sender.sendPlainMessage("No subcommand given")
        else if (args.size == 1 && args[0] == "unregister") {
            if (playerServicesConfig.contains(sender.name)) {
                playerServicesConfig[sender.name] = null
                parentPlugin.saveConfig()
                return true
            }
        } else if (args.size == 2 && args[0] == "register") {
            try {
                val serviceUrl = URL(args[1])
                playerServicesConfig[sender.name] = RegisteredService(sender.uniqueId, serviceUrl)
                parentPlugin.saveConfig()
                return true
            } catch (ex: MalformedURLException) {
                sender.sendPlainMessage("Invalid service URL")
            }
        }
        return false
    }

    private fun handleUserCommandPrivacyMode(): Boolean = false
    private fun handleUserCommandSharingMode(): Boolean = false
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
