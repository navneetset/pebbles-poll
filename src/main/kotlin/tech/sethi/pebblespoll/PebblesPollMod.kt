package tech.sethi.pebblespoll

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

class PebblesPollMod : ModInitializer {
    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            PebblesPollInitializer.register(dispatcher)
        })
    }
}
