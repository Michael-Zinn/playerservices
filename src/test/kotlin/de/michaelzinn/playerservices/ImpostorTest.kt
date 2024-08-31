package de.michaelzinn.playerservices

import de.michaelzinn.playerservices.data.PlayerServiceEntry
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.*

private val NOTCH_PLAYER_UUID = UUID.fromString("0-0-0-0-0")
private val IMPOSTOR_PLAYER_UUID = UUID.fromString("1-1-1-1-1")

class ImpostorTest : MockedPluginTest() {
    @Test
    fun `can't re-register a service with a different player UUID`() = test {
        val notch = player("Notch", NOTCH_PLAYER_UUID)
        givenRegisteredPlayerServices(notch to "http://example.com/playerservice")

        val impostor = player("Notch", IMPOSTOR_PLAYER_UUID)
        val isCommandSuccessful = impostor types "/ps register http://example.com/impostor-took-your-service"

        isCommandSuccessful shouldBe false
        playerServiceRegistryPersistence.get().values shouldBe listOf(
            PlayerServiceEntry(
                playerName = "Notch",
                playerUuid = notch.uniqueId.toString(),
                serviceUrl = "http://example.com/playerservice",
            )
        )
        /*
        shouldContainExactly mapOf(
            "Notch" to RegisteredService(
                notch.uniqueId,
                URL("http://example.com/playerservice")
            )
        )

         */
    }

    @Test
    fun `can't unregister a service with a different player UUID`() = test {
        val notch = player("Notch", NOTCH_PLAYER_UUID)
        givenRegisteredPlayerServices(notch to "http://example.com/playerservice")

        val impostor = player("Notch", IMPOSTOR_PLAYER_UUID)
        val isCommandSuccessful = impostor types "/ps unregister"

        isCommandSuccessful shouldBe false
        playerServiceRegistryPersistence.get().values shouldBe listOf(
            PlayerServiceEntry(
                playerName = "Notch",
                playerUuid = notch.uniqueId.toString(),
                serviceUrl = "http://example.com/playerservice",
            )
        )
        /*configurationSection.getValues(true) shouldContainExactly mapOf(
            "Notch" to RegisteredService(
                notch.uniqueId,
                URL("http://example.com/playerservice")
            )
        )*/
    }
}