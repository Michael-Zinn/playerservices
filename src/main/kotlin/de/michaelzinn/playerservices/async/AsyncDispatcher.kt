package de.michaelzinn.playerservices.async

import kotlinx.coroutines.CoroutineDispatcher
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import kotlin.coroutines.CoroutineContext

class AsyncDispatcher(
    val parentPlugin: Plugin,
    val scheduler: BukkitScheduler,
) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        scheduler.runTaskAsynchronously(parentPlugin, block)
    }
}

