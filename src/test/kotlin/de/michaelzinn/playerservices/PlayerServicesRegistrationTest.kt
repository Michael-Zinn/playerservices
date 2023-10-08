package de.michaelzinn.playerservices

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.bukkit.command.PluginCommand
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.entity.Player
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URL
import java.util.*

class PlayerServicesRegistrationTest {
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

    // TODO: Register an IP address
    @Test
    fun `registers a player service`() {
        val notch = player("Notch")

        val isCommandSuccessful = notch types "/ps register http://example.com/playerservice"

        isCommandSuccessful shouldBe true
        verifyOrder {
            configurationSection.set(any(), any())
            playerServices.saveConfig()
        }
        configurationSection.getValues(false) shouldContainExactly mapOf(
            "Notch" to RegisteredService(
                notch.uniqueId,
                URL("http://example.com/playerservice")
            )
        )
    }

    @Test
    fun `registers one service per player`() {
        "Notch" types "/ps register http://example.com/notchplayerservice"
        "Herobrine" types "/ps register http://example.com/herobrineplayerservice"

        configurationSection.getKeys(true) shouldContainExactlyInAnyOrder setOf(
            "Notch",
            "Herobrine"
        )
    }

    @Test
    fun `re-registration overwrites previous service`() {
        val notch = player("Notch")

        notch types "/ps register http://example.com/v1/playerservice"
        notch types "/ps register http://example.com/v2/playerservice"

        configurationSection.getValues(false) shouldContainExactly mapOf(
            "Notch" to RegisteredService(
                notch.uniqueId,
                URL("http://example.com/v2/playerservice")
            )
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "0://example.com/", "://example.com/"])
    fun `rejects an invalid URL`(invalidUrl: String) {
        val isCommandSuccessful = "Notch" types "/ps register $invalidUrl"
        isCommandSuccessful shouldBe false
        configurationSection.getValues(true) shouldHaveSize 0
    }

    @Test
    fun `rejects empty admin command`() {
        val isCommandSuccessful = "Notch" types ""
        isCommandSuccessful shouldBe false
    }

    @Test
    fun `rejects an unknown admin command`() {
        val isCommandSuccessful = "Notch" types "/ps pspsps"
        isCommandSuccessful shouldBe false
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
