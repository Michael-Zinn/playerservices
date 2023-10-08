package de.michaelzinn.playerservices

import io.kotest.matchers.collections.shouldContainExactly
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

class PlayerServicesUnregistrationTest {
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
    fun `unregisters a player's own service`() {
        val notch = player("Notch")
        val herobrine = player("Herobrine")
        givenRegisteredPlayerServices(
            notch to URL("http://example.com/notchplayerservice"),
            herobrine to URL("http://example.com/herobrineplayerservice")
        )

        val isCommandSuccessful = notch types "/ps unregister"

        isCommandSuccessful shouldBe true
        verifyOrder {
            configurationSection.set(any(), null)
            playerServices.saveConfig()
        }
        configurationSection.getKeys(true) shouldContainExactly setOf("Herobrine")
    }

    @Test
    fun `unregistering without a player service does nothing`() {
        givenRegisteredPlayerServices(
            player("Herobrine") to URL("http://example.com/herobrineplayerservice")
        )

        val isCommandSuccessful = "Notch" types "/ps unregister"

        isCommandSuccessful shouldBe false
        configurationSection.getKeys(true) shouldContainExactly setOf("Herobrine")
    }

    private fun givenRegisteredPlayerServices(vararg registeredServices: Pair<Player, URL>) {
        registeredServices.forEach {
            configurationSection[it.first.name] = RegisteredService(it.first.uniqueId, it.second)
        }
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
