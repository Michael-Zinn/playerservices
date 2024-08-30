package de.michaelzinn.playerservices

import de.michaelzinn.playerservices.data.RegisteredService
import de.michaelzinn.playerservices.net.PlayerServiceClient
import de.michaelzinn.playerservices.util.Ok
import io.mockk.*
import org.bukkit.command.CommandSender
import org.bukkit.command.PluginCommand
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask
import org.junit.jupiter.api.BeforeEach
import java.net.URL
import java.util.*

open class MockedPluginTest {
    private lateinit var commandExecutor: PlayerServicesCommandExecutor

    protected lateinit var client: PlayerServiceClient
    protected lateinit var configurationSection: ConfigurationSection
    protected lateinit var playerServices: PlayerServices
    protected lateinit var scheduler: BukkitScheduler

    @BeforeEach
    fun setUpMocks() {
        clearAllMocks()

        client = buildClientMock()
        configurationSection = spyk(MemoryConfiguration())
        playerServices = buildPlayerServicesMock()
        scheduler = buildBukkitSchedulerMock()

        commandExecutor = PlayerServicesCommandExecutor(playerServices, configurationSection, client, scheduler)
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

    private fun buildClientMock(): PlayerServiceClient = mockk {
        every { register(any()) } returns Ok()
    }

    private fun buildBukkitSchedulerMock(): BukkitScheduler {
        val scheduler = mockk<BukkitScheduler>()
        every { scheduler.runTaskAsynchronously(any<Plugin>(), any<Runnable>()) } answers {
            secondArg<Runnable>().run()
            mockk<BukkitTask>()
        }
        every { scheduler.runTask(any<Plugin>(), any<Runnable>()) } answers {
            secondArg<Runnable>().run()
            mockk<BukkitTask>()
        }

        return scheduler
    }

    protected fun givenRegisteredPlayerServices(vararg registeredServices: Pair<Player, String>) {
        registeredServices.forEach {
            configurationSection[it.first.name] = RegisteredService(it.first.uniqueId, URL(it.second))
        }
    }

    protected infix fun String.startsTyping(input: String) = player(this@startsTyping) startsTyping input

    protected infix fun CommandSender.startsTyping(input: String): MutableList<String>? {
        val (command, args) = splitIntoCommandAndArgs(input)

        val pluginCommand: PluginCommand = mockk {
            every { name } returns command
        }

        return commandExecutor.onTabCompete(this@startsTyping, pluginCommand, args)
    }

    protected infix fun String.types(input: String) = player(this@types) types input

    protected fun String.types(input: String, callback: (Boolean) -> Unit) = player(this@types).types(input, callback)

    protected infix fun CommandSender.types(input: String): Boolean {
        val (command, args) = splitIntoCommandAndArgs(input)

        val pluginCommand: PluginCommand = mockk {
            every { name } returns command
        }

        return commandExecutor.onCommand(this@types, pluginCommand, input, args)
    }

    protected fun CommandSender.types(input: String, callback: (Boolean) -> Unit) {
        val (command, args) = splitIntoCommandAndArgs(input)

        val pluginCommand: PluginCommand = mockk {
            every { name } returns command
        }

        commandExecutor.onCommandAsync(this@types, pluginCommand, input, args, callback)
    }


    protected fun player(name: String, uniqueId: UUID = UUID.randomUUID()): Player = mockk {
        every { server } returns playerServices.server
        every { getName() } returns name
        every { getUniqueId() } returns uniqueId
        every { sendPlainMessage(any()) } just runs
        every { sendRichMessage(any()) } just runs
    }

    private fun splitIntoCommandAndArgs(input: String): Pair<String, Array<String>> {
        val inputParts = input.removePrefix("/").split(" ")
        val command = inputParts.first()
        val args = inputParts.drop(1).toTypedArray()
        return Pair(command, args)
    }
}