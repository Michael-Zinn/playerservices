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