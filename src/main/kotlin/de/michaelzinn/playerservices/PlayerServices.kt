package de.michaelzinn.playerservices

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class PlayerServices : JavaPlugin() {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        logger.info("Command $label (alias for ${command.name}) requested on ${this.server.name}, ${this.server.ip}, ${this.server.port} by ${sender.name}")
        return when (command.name) {
            "ps" -> handleAdminCommand()
            "p" -> handleUserCommandPrivacyMode()
            "s" -> handleUserCommandSharingMode()
            else -> false
        }
    }

    private fun handleAdminCommand(): Boolean = false
    private fun handleUserCommandPrivacyMode(): Boolean = false
    private fun handleUserCommandSharingMode(): Boolean = false
}
