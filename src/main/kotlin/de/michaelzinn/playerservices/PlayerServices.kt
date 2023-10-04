package de.michaelzinn.playerservices

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused") // Instantiated by the server
class PlayerServices : JavaPlugin() {
    private lateinit var delegate: PlayerServicesCommandExecutor

    override fun onLoad() {
        ConfigurationSerialization.registerClass(ServiceInfo::class.java)
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
        return false
    }

    private fun handleUserCommandPrivacyMode(): Boolean = false
    private fun handleUserCommandSharingMode(): Boolean = false
}
