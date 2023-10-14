package de.michaelzinn.playerservices

import io.kotest.matchers.shouldBe
import io.mockk.*
import org.bukkit.craftbukkit.v1_20_R1.command.CraftConsoleCommandSender
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class UsageTest : MockedPluginTest() {
    @ParameterizedTest
    @ValueSource(strings = ["/p", "/s"])
    fun `calls existing service`(command: String) {
        givenRegisteredPlayerServices(player("Herobrine") to "http://example.com/playerservice")
        val notch = player("Notch")

        val isCommandSuccessful = notch types "$command Herobrine"

        isCommandSuccessful shouldBe true
        verify {
            notch.sendPlainMessage(any())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["/p", "/s"])
    fun `calls service when partial owner name given`(command: String) {
        givenRegisteredPlayerServices(player("Herobrine") to "http://example.com/playerservice")

        val isCommandSuccessful = "Notch" types "$command hero"

        isCommandSuccessful shouldBe true
    }

    @ParameterizedTest
    @ValueSource(strings = ["/p", "/s"])
    fun `exact owner name match wins over partial match`(command: String) {
        givenRegisteredPlayerServices(
            player("Player") to "http://example.com/playerservice",
            player("PlayerWithSuffix") to "http://example.com/otherservice"
        )

        val isCommandSuccessful = "Notch" types "$command Player"

        isCommandSuccessful shouldBe true
        // TODO: When a real HTTP client is used, verify service of "Player" was called
    }

    @ParameterizedTest
    @ValueSource(strings = ["/p", "/s"])
    fun `rejects ambiguous partial owner name`(command: String) {
        givenRegisteredPlayerServices(
            player("Player1") to "http://example.com/player1service",
            player("player2") to "http://example.com/player2service"
        )

        val isCommandSuccessful = "Notch" types "$command p"

        isCommandSuccessful shouldBe false
    }

    @ParameterizedTest
    @ValueSource(strings = ["/p", "/s"])
    fun `ignores non-existing service`(command: String) {
        val isCommandSuccessful = "Notch" types "$command Herobrine"
        isCommandSuccessful shouldBe false
    }

    @Test
    fun `privacy mode only sends player name, id, command and parameters to a service`() {
        // TODO: Implement test when a real HTTP client is used
    }

    @Test
    fun `sharing mode sends data of privacy mode plus player coordinates, dimension, yaw and pitch`() {
        // TODO: Implement test when a real HTTP client is used
    }

    @ParameterizedTest
    @ValueSource(strings = ["/p", "/s"])
    fun `rejects non-player callers`(command: String) {
        givenRegisteredPlayerServices(player("Herobrine") to "http://example.com/playerservice")
        val serverConsole = mockk<CraftConsoleCommandSender> {
            every { name } returns "CONSOLE"
            every { sendPlainMessage(any()) } just runs
        }

        val isCommandSuccessful = serverConsole types "$command Herobrine"

        isCommandSuccessful shouldBe false
    }

    @ParameterizedTest
    @ValueSource(strings = ["/p", "/s"])
    fun `rejects empty user command`(command: String) {
        val isCommandSuccessful = "Notch" types command
        isCommandSuccessful shouldBe false
    }
}