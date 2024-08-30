package de.michaelzinn.playerservices.util

import org.bukkit.entity.Player

fun Player.sendErrorMessage(message: String) = this.sendRichMessage("<red>Error:</red> $message")
