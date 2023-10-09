package de.michaelzinn.playerservices

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test

class UnregistrationTest : MockedPluginTest() {
    @Test
    fun `unregisters a player's own service`() {
        val notch = player("Notch")
        val herobrine = player("Herobrine")
        givenRegisteredPlayerServices(
            notch to "http://example.com/notchplayerservice",
            herobrine to "http://example.com/herobrineplayerservice"
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
            player("Herobrine") to "http://example.com/herobrineplayerservice"
        )

        val isCommandSuccessful = "Notch" types "/ps unregister"

        isCommandSuccessful shouldBe false
        configurationSection.getKeys(true) shouldContainExactly setOf("Herobrine")
    }


}
