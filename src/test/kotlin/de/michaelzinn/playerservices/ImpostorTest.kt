package de.michaelzinn.playerservices

import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.bukkit.command.PluginCommand
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.entity.Player
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URL
import java.util.*

private const val NOTCH_PLAYER_UUID = "0-0-0-0-0"
private const val IMPOSTOR_PLAYER_UUID = "1-1-1-1-1"

class ImpostorTest {
    private lateinit var commandExecutor: PlayerServicesCommandExecutor

    private lateinit var configurationSection: ConfigurationSection
    private lateinit var playerServices: PlayerServices

    @BeforeEach
    fun setUpMocks() {
        clearAllMocks()

        configurationSection = spyk(MemoryConfiguration())

        playerServices = mockk {
            every { server } returns mockk {
                every { name } returns "Testserver"
                every { ip } returns "127.0.0.1"
                every { port } returns 1337
            }
            every { logger } returns mockk {
                every { info(any(String::class)) } just runs
            }
            every { saveConfig() } just runs
        }

        commandExecutor = PlayerServicesCommandExecutor(configurationSection, playerServices)
    }

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

    private fun givenRegisteredPlayerServices(vararg registeredServices: Pair<Player, String>) {
        registeredServices.forEach {
            configurationSection[it.first.name] = RegisteredService(it.first.uniqueId, URL(it.second))
        }
    }

    private fun player(name: String, uniqueId: String): Player = mockk {
        every { getName() } returns name
        every { getUniqueId() } returns UUID.fromString(uniqueId)
        every { sendRichMessage(any()) } just runs
    }

    private infix fun Player.types(input: String): Boolean {
        val inputParts = input.removePrefix("/").split(" ")
        val command = inputParts.first()
        val args = inputParts.drop(1).toTypedArray()

        val pluginCommand: PluginCommand = mockk {
            every { name } returns command
        }

        return commandExecutor.onCommand(this@types, pluginCommand, input, args)
    }
}