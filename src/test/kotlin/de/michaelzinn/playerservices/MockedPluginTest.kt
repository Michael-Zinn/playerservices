package de.michaelzinn.playerservices

import io.mockk.*
import org.bukkit.command.PluginCommand
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.entity.Player
import org.junit.jupiter.api.BeforeEach
import java.net.URL
import java.util.*

open class MockedPluginTest {
    private lateinit var commandExecutor: PlayerServicesCommandExecutor

    protected lateinit var configurationSection: ConfigurationSection
    protected lateinit var playerServices: PlayerServices

    @BeforeEach
    fun setUpMocks() {
        clearAllMocks()

        configurationSection = spyk(MemoryConfiguration())
        playerServices = buildPlayerServicesMock()
        commandExecutor = PlayerServicesCommandExecutor(configurationSection, playerServices)
    }

    private fun buildPlayerServicesMock(): PlayerServices = mockk {
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

    protected fun givenRegisteredPlayerServices(vararg registeredServices: Pair<Player, String>) {
        registeredServices.forEach {
            configurationSection[it.first.name] = RegisteredService(it.first.uniqueId, URL(it.second))
        }
    }

    protected infix fun String.types(input: String) = player(this@types) types input

    protected fun player(name: String, uniqueId: UUID = UUID.randomUUID()): Player = mockk {
        every { getName() } returns name
        every { getUniqueId() } returns uniqueId
        every { sendRichMessage(any()) } just runs
    }

    protected infix fun Player.types(input: String): Boolean {
        val inputParts = input.removePrefix("/").split(" ")
        val command = inputParts.first()
        val args = inputParts.drop(1).toTypedArray()

        val pluginCommand: PluginCommand = mockk {
            every { name } returns command
        }

        return commandExecutor.onCommand(this@types, pluginCommand, input, args)
    }
}