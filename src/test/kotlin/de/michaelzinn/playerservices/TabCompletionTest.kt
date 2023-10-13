package de.michaelzinn.playerservices

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.mockk
import org.bukkit.craftbukkit.v1_20_R1.command.CraftConsoleCommandSender
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TabCompletionTest : MockedPluginTest() {
    @Test
    fun `suggests subcommand 'register' when not registered`() {
        val suggestions = "Notch" startsTyping "/ps "
        suggestions shouldContainExactly listOf("register")
    }

    @Test
    fun `suggests subcommand 'unregister' when registered`() {
        val notch = player("Notch")
        givenRegisteredPlayerServices(notch to "http://example.com/playerservice")

        val suggestions = notch startsTyping "/ps "

        suggestions shouldContainExactly listOf("unregister")
    }

    @Test
    fun `suggests no URL when registering`() {
        val suggestions = player("Notch") startsTyping "/ps register http"
        suggestions.shouldBeNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["/p", "/s"])
    fun `suggests service owners`(command: String) {
        givenRegisteredPlayerServices(
            player("Notch") to "http://example.com/notchplayerservice",
            player("Herobrine") to "http://example.com/herobrineplayerservice"
        )

        val suggestions = "Notch" startsTyping "$command "

        suggestions shouldContainExactlyInAnyOrder listOf("Notch", "Herobrine")
    }

    @ParameterizedTest
    @ValueSource(strings = ["/p", "/s"])
    fun `suggests up to ten completions`(command: String) {
        for (i in 1..50)
            givenRegisteredPlayerServices(player("Player$i") to "http://example.com/playerservice$i")

        val suggestions = "Notch" startsTyping "$command "

        suggestions.shouldNotBeNull()
        suggestions shouldHaveSize 10
    }

    @ParameterizedTest
    @ValueSource(strings = ["/p", "/s"])
    fun `suggests only service owners where prefix matches`(command: String) {
        givenRegisteredPlayerServices(
            player("Notch") to "http://example.com/notchplayerservice",
            player("Herobrine") to "http://example.com/herobrineplayerservice"
        )

        val suggestions = "Notch" startsTyping "$command h"

        suggestions shouldContainExactlyInAnyOrder listOf("Herobrine")
    }

    @ParameterizedTest
    @ValueSource(strings = ["/p", "/s"])
    fun `suggests no service arguments`(command: String) {
        givenRegisteredPlayerServices(player("Herobrine") to "http://example.com/playerservice")

        val suggestions = "Notch" startsTyping "$command Herobrine "

        suggestions.shouldBeNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["/ps", "/p", "/s"])
    fun `suggests completions only to players`(command: String) {
        val serverConsole = mockk<CraftConsoleCommandSender>()

        val suggestions = serverConsole startsTyping command

        suggestions.shouldBeNull()
    }
}