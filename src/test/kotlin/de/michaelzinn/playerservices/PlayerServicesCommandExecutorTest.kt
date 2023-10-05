package de.michaelzinn.playerservices

import io.kotest.matchers.shouldBe
import io.mockk.*
import org.bukkit.command.PluginCommand
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URL
import java.util.*

class PlayerServicesCommandExecutorTest {
    private lateinit var commandExecutor: PlayerServicesCommandExecutor

    private val configSectionSetPath = slot<String>()
    private val configSectionSetValue = slot<RegisteredService>()

    @BeforeEach
    fun setUpMocks() {
        val configSectionMock: ConfigurationSection = mockk {
            every { set(capture(configSectionSetPath), capture(configSectionSetValue)) } just runs
        }

        val playerServicesMock: PlayerServices = mockk {
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

        commandExecutor = PlayerServicesCommandExecutor(configSectionMock, playerServicesMock)
    }

    // TODO: Register an IP address
    @Test
    fun `registers a player service`() {
        val notch = player("Notch")

        val result = notch types "/ps register http://example.com/playerservice"

        result shouldBe true
        configSectionSetPath.captured shouldBe "Notch"
        configSectionSetValue.captured.ownerId shouldBe notch.uniqueId
        configSectionSetValue.captured.url shouldBe URL("http://example.com/playerservice")
        // TODO: verify immediate call to saveConfig()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "0://example.com/", "://example.com/"])
    fun `rejects an invalid URL`(invalidUrl: String) {
        val result = "Notch" types "/ps register $invalidUrl"
        result shouldBe false
    }

    @Test
    fun `rejects empty admin command`() {
        val result = "Notch" types ""
        result shouldBe false
    }

    @Test
    fun `rejects an unknown admin command`() {
        val result = "Notch" types "/ps pspsps"
        result shouldBe false
    }

    private infix fun String.types(input: String) = player(this@types) types input

    private fun player(name: String): Player = mockk {
        every { getName() } returns name
        every { uniqueId } returns UUID.randomUUID()
        every { sendPlainMessage(any()) } just runs
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
