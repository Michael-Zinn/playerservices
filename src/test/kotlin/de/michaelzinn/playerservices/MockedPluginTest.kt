package de.michaelzinn.playerservices

import com.github.michaelbull.result.Ok
import de.michaelzinn.playerservices.async.MainThreadDispatcher
import de.michaelzinn.playerservices.data.PlayerServiceEntry
import de.michaelzinn.playerservices.net.PlayerServiceClient
import de.michaelzinn.playerservices.persistence.PlayerServiceRegistry
import de.michaelzinn.playerservices.persistence.PlayerServiceRegistryPersistence
import de.michaelzinn.playerservices.util.Ok
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.command.PluginCommand
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

    protected lateinit var playerServices: PlayerServices
    protected lateinit var playerServiceRegistryPersistence: PlayerServiceRegistryPersistence
    protected lateinit var scheduler: BukkitScheduler
    protected lateinit var mockedWorld: World

    protected lateinit var testMainDispatcher: MainThreadDispatcher

    @BeforeEach
    fun setUpMocks() {
        clearAllMocks()

        playerServices = buildPlayerServicesMock()
        playerServiceRegistryPersistence = buildPlayerServiceRegistryPersistence()
        client = buildClientMock()
        scheduler = buildBukkitSchedulerMock()

        mockedWorld = buildWorldMock()

        testMainDispatcher = MainThreadDispatcher(playerServices, scheduler)

        commandExecutor = PlayerServicesCommandExecutor(
            parentPlugin = playerServices,
            registry = PlayerServiceRegistry(playerServiceRegistryPersistence),
            client = client,
            scheduler = scheduler
        )
    }

    fun test(code: suspend () -> Unit) {
        CoroutineScope(testMainDispatcher).launch {
            code()
        }
        return
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

    private fun buildPlayerServiceRegistryPersistence(): PlayerServiceRegistryPersistence = mockk {
        var entries: MutableMap<String, PlayerServiceEntry> = mutableMapOf()
        every { add(any()) } answers {
            val entry = it.invocation.args[0] as PlayerServiceEntry
            entries.put(entry.playerName, entry)
        }
        every { remove(any()) } answers {
            entries.remove(it.invocation.args[0])
        }
        every { get() } returns entries
    }

    private fun buildClientMock(): PlayerServiceClient = mockk {
        every { register(any()) } returns Ok()
        every { sharingRequest(any<String>(), any()) } returns Ok("Sharing Ok!")
        every { sharingRequest(any<URL>(), any()) } returns Ok("Sharing Ok!")
        every { privateRequest() } returns Ok("Private Ok!")
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

    private fun buildWorldMock(): World = mockk {
        every { name } returns "World"
    }

    protected fun givenRegisteredPlayerServices(vararg registeredServices: Pair<Player, String>) {
        registeredServices.map { (player, url) ->
            PlayerServiceEntry(
                playerName = player.name,
                playerUuid = player.uniqueId.toString(),
                serviceUrl = url,
            )
        }.forEach { entry ->
            playerServiceRegistryPersistence.add(entry)
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

    protected suspend infix fun String.types(input: String) = player(this@types) types input

    protected suspend infix fun CommandSender.types(input: String): Boolean {
        val (command, args) = splitIntoCommandAndArgs(input)

        val pluginCommand: PluginCommand = mockk {
            every { name } returns command
        }

        return commandExecutor.onCommandAsync(this@types, pluginCommand, input, args)
    }

    protected fun player(name: String, uniqueId: UUID = UUID.randomUUID()): Player = mockk {
        every { server } returns playerServices.server
        every { getName() } returns name
        every { getUniqueId() } returns uniqueId
        every { sendPlainMessage(any()) } just runs
        every { sendRichMessage(any()) } just runs

        every { world } returns mockedWorld
        every { x } returns 0.0
        every { y } returns 118.0
        every { z } returns 0.0
        every { pitch } returns 0.0f
        every { yaw } returns 0.0f

    }

    private fun splitIntoCommandAndArgs(input: String): Pair<String, Array<String>> {
        val inputParts = input.removePrefix("/").split(" ")
        val command = inputParts.first()
        val args = inputParts.drop(1).toTypedArray()
        return Pair(command, args)
    }
}