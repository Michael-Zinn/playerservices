package de.michaelzinn.playerservices

import de.michaelzinn.playerservices.data.PlayerServiceEntry
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.bukkit.craftbukkit.v1_20_R1.command.CraftConsoleCommandSender
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RegistrationTest : MockedPluginTest() {
    @ParameterizedTest
    @ValueSource(
        strings = [
            "http://example.com/playerservice",
            "http://127.0.0.1/playerservice",
            "http://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]/playerservice",
            "http://[::1]/playerservice"]
    )
    fun `registers a player service`(validUrl: String) = test {

        val notch = player("Notch")

        val isCommandSuccessful = notch types "/ps register $validUrl"

        isCommandSuccessful shouldBe true
        playerServiceRegistryPersistence.get().values shouldBe listOf(
            PlayerServiceEntry(
                playerName = "Notch",
                playerUuid = notch.uniqueId.toString(),
                serviceUrl = validUrl,
            )
        )
    }

    @Test
    fun `registers one service per player`() = test {
        "Notch" types "/ps register http://example.com/notchplayerservice"
        "Herobrine" types "/ps register http://example.com/herobrineplayerservice"

        playerServiceRegistryPersistence.get().keys shouldContainExactlyInAnyOrder setOf(
            "Notch",
            "Herobrine"
        )
    }

    @Test
    fun `re-registration overwrites previous service`() = test {
        val notch = player("Notch")

        notch types "/ps register http://example.com/v1/playerservice"
        notch types "/ps register http://example.com/v2/playerservice"
        playerServiceRegistryPersistence.get().values shouldBe listOf(
            PlayerServiceEntry(
                playerName = "Notch",
                playerUuid = notch.uniqueId.toString(),
                serviceUrl = "http://example.com/v2/playerservice"
            )
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "0://example.com/",
            "://example.com/",
            "http://[:::1]/playerservice"]
    )
    fun `rejects an invalid URL`(invalidUrl: String) = test {
        val isCommandSuccessful = "Notch" types "/ps register $invalidUrl"
        isCommandSuccessful shouldBe false
        playerServiceRegistryPersistence.get() shouldHaveSize 0
    }

    @Test
    fun `rejects empty command`() = test {
        val isCommandSuccessful = "Notch" types ""
        isCommandSuccessful shouldBe false
    }

    @Test
    fun `rejects empty registration command`() = test {
        val isCommandSuccessful = "Notch" types "/ps"
        isCommandSuccessful shouldBe false
    }

    @Test
    fun `rejects an unknown registration command`() = test {
        val isCommandSuccessful = "Notch" types "/ps pspsps"
        isCommandSuccessful shouldBe false
    }

    @Test
    fun `rejects non-players`() = test {
        val serverConsole = mockk<CraftConsoleCommandSender> {
            every { name } returns "CONSOLE"
            every { sendPlainMessage(any()) } just runs
        }

        val isCommandSuccessful = serverConsole types "/ps register http://example.com/playerservice"

        isCommandSuccessful shouldBe false
    }
}
