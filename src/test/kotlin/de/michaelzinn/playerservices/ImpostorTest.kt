package de.michaelzinn.playerservices

import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.net.URL
import java.util.*

private val NOTCH_PLAYER_UUID = UUID.fromString("0-0-0-0-0")
private val IMPOSTOR_PLAYER_UUID = UUID.fromString("1-1-1-1-1")

class ImpostorTest : MockedPluginTest() {
    @Test
    fun `can't re-register a service with a different player UUID`() {
        val notch = player("Notch", NOTCH_PLAYER_UUID)
        givenRegisteredPlayerServices(notch to "http://example.com/playerservice")

        val impostor = player("Notch", IMPOSTOR_PLAYER_UUID)
        val isCommandSuccessful = impostor types "/ps register http://example.com/impostor-took-your-service"

        isCommandSuccessful shouldBe false
        configurationSection.getValues(true) shouldContainExactly mapOf(
            "Notch" to RegisteredService(
                notch.uniqueId,
                URL("http://example.com/playerservice")
            )
        )
    }

    @Test
    fun `can't unregister a service with a different player UUID`() {
        val notch = player("Notch", NOTCH_PLAYER_UUID)
        givenRegisteredPlayerServices(notch to "http://example.com/playerservice")

        val impostor = player("Notch", IMPOSTOR_PLAYER_UUID)
        val isCommandSuccessful = impostor types "/ps unregister"

        isCommandSuccessful shouldBe false
        configurationSection.getValues(true) shouldContainExactly mapOf(
            "Notch" to RegisteredService(
                notch.uniqueId,
                URL("http://example.com/playerservice")
            )
        )
    }
}