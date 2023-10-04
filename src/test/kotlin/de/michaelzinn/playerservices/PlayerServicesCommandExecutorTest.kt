package de.michaelzinn.playerservices

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.bukkit.command.PluginCommand
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlayerServicesCommandExecutorTest {
    private lateinit var commandExecutor: PlayerServicesCommandExecutor

    @BeforeEach
    fun setUpMocks() {
        val configSectionMock: ConfigurationSection = mockk()

        val playerServicesMock: PlayerServices = mockk {
            every { server } returns mockk {
                every { name } returns "Testserver"
                every { ip } returns "127.0.0.1"
                every { port } returns 1337
            }
            every { logger } returns mockk {
                every { info(any(String::class)) } just runs
            }
        }

        commandExecutor = PlayerServicesCommandExecutor(configSectionMock, playerServicesMock)
    }

    @Test
    fun `rejects empty admin command`() {
        val actual = whenPlayerTypesCommand("Notch", "")

        actual shouldBe false
    }

    private fun whenPlayerTypesCommand(playerName: String, command: String): Boolean {
        val player: Player = mockk {
            every { name } returns playerName
        }
        val pluginCommand: PluginCommand = mockk {
            every { name } returns command
        }

        return commandExecutor.onCommand(player, pluginCommand, command, null)
    }
}
