package de.michaelzinn.playerservices

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.mockk.mockk
import org.bukkit.craftbukkit.v1_20_R1.command.CraftConsoleCommandSender
import org.junit.jupiter.api.Test

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
    fun `suggests only the first argument`() {
        val suggestions = player("Notch") startsTyping "/ps register http"
        suggestions.shouldBeNull()
    }

    @Test
    fun `suggests completions only to players`() {
        val serverConsole = mockk<CraftConsoleCommandSender>()

        val suggestions = serverConsole startsTyping "/ps"

        suggestions.shouldBeNull()
    }
}