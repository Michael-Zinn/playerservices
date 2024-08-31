package de.michaelzinn.playerservices.persistence

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import de.michaelzinn.playerservices.data.PlayerServiceEntry
import org.bukkit.entity.Player
import java.net.URL
import java.util.*

class PlayerServiceRegistry(
    val persistence: PlayerServiceRegistryPersistence,
) {

    fun getNames(): List<String> {
        val names = persistence.get().keys.sorted()
        return names
    }

    fun contains(playerName: String) = getNames().contains(playerName)

    fun getServiceByExactName(playerName: String) = persistence.get()[playerName]

    fun searchServiceByOwner(searchedServiceOwner: String, player: Player): Result<PlayerServiceEntry, String> {
        val exactMatch = getServiceByExactName(searchedServiceOwner)
        if (exactMatch != null) return Ok(exactMatch)

        val partialMatches = completeServiceOwnerNames(searchedServiceOwner)

        return when {
            partialMatches.isEmpty() -> Err(
                "No service registered for player $searchedServiceOwner"
            )

            partialMatches.size > 1 -> Err(
                "Player name $searchedServiceOwner is ambiguous, first ${partialMatches.size} candidates: ${partialMatches.joinToString()}"
            )

            else -> Ok(
                getServiceByExactName(partialMatches.first())!!
            )
        }
    }

    fun completeServiceOwnerNames(searchedOwnerName: String) =
        getNames()
            .filter { it.startsWith(searchedOwnerName, ignoreCase = true) }
            .take(10)

    fun hasDifferentPlayerUuid(sender: Player): Boolean {
        val currentOwnerId = getServiceByExactName(sender.name)?.playerUuid
        val hasPlayerUuidChanged = currentOwnerId?.let { it != sender.uniqueId.toString() }
        return hasPlayerUuidChanged ?: false
    }

    fun register(
        playerName: String,
        playerUuid: UUID,
        serviceUrl: URL,
    ) = register(
        PlayerServiceEntry(
            playerName = playerName,
            playerUuid = playerUuid.toString(),
            serviceUrl = serviceUrl.toString(),
        )
    )

    fun register(service: PlayerServiceEntry) {
        persistence.add(service)
    }

    fun unregister(player: Player): Boolean {
        if (!contains(player.name)) return false
        if (hasDifferentPlayerUuid(player)) return false

        persistence.remove(player.name)

        return true
    }

}