package tech.sethi.pebblespoll

import Poll
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.concurrent.ConcurrentHashMap


object PebblesPollInitializer {
    private val activePolls = ConcurrentHashMap<String, Poll>()
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(literal<ServerCommandSource>("pebblespoll")
            .then(literal("create")
                .requires { it.hasPermissionLevel(2) }
                .then(argument("question", greedyString())
                    .executes { context ->
                        val question = getString(context, "question")
                        val source = context.source
                        if (activePolls.containsKey(source.server.name)) {
                            source.sendError(Text.literal("A poll is already active!").formatted(Formatting.RED))
                        } else {
                            val poll = Poll(question)
                            activePolls[source.server.name] = poll
                            source.sendFeedback(Text.literal("Poll created! Set the duration with /pebblespoll time [minutes]").formatted(Formatting.GREEN), false)
                        }
                        1
                    }))
            .then(literal("remind")
                .requires { it.hasPermissionLevel(2) }
                .executes { context ->
                    val source = context.source
                    val poll = activePolls[source.server.name]
                    if (poll != null) {
                        poll.displayQuestionAndOptions(source)
                    } else {
                        source.sendError(Text.literal("No active poll found!").formatted(Formatting.RED))
                    }
                    1
                }
            )
            .then(literal("time")
                .requires { it.hasPermissionLevel(2) }
                .then(argument("duration", greedyString())
                    .executes { context ->
                        val duration = getString(context, "duration").toInt()
                        val source = context.source
                        val poll = activePolls[source.server.name]
                        if (poll != null) {
                            poll.duration = duration
                            source.sendFeedback(Text.literal("Poll duration set to $duration minutes! Set the options (separated by commas) with /pebblespoll options [options]").formatted(Formatting.GREEN), false)
                        } else {
                            source.sendError(Text.literal("No active poll found!").formatted(Formatting.RED))
                        }
                        1
                    }))
            .then(literal("options")
                .requires { it.hasPermissionLevel(2) }
                .then(argument("options", greedyString())
                    .executes { context ->
                        try {
                            val optionsString = getString(context, "options")
                            val source = context.source
                            val poll = activePolls[source.server.name]
                            if (poll != null) {
                                val options = optionsString.split(",") // Split options using comma as a delimiter
                                for (option in options) {
                                    poll.addOption(option.trim()) // Add the option after trimming any leading or trailing whitespace
                                }
                                source.sendFeedback(Text.literal("Poll options set!").formatted(Formatting.GREEN), false)

                                // Start the poll and display it to the players
                                poll.start(source) {
                                    activePolls.remove(source.server.name)
                                }

                            } else {
                                source.sendError(Text.literal("No active poll found!").formatted(Formatting.RED))
                            }
                        } catch (e: Exception) {
                            // Print the exception to get more information about the error
                            e.printStackTrace()
                        }
                        1
                    }))
            .then(literal("end")
                .requires { it.hasPermissionLevel(2) }
                .executes { context ->
                    val source = context.source
                    val poll = activePolls[source.server.name]
                    if (poll != null) {
                        poll.displayResults(source)
                        activePolls.remove(source.server.name)
                        source.sendFeedback(Text.literal("Poll forcefully ended!").formatted(Formatting.GREEN), false)
                    } else {
                        source.sendError(Text.literal("No active poll found!").formatted(Formatting.RED))
                    }
                    1
                }
            )
            .then(literal("clear")
                .requires { it.hasPermissionLevel(2) }
                .executes { context ->
                    val source = context.source
                    val scoreboard = source.server.scoreboard
                    val pollResultsObjective = scoreboard.getObjective("Poll")
                    val remainingTimeObjective = scoreboard.getObjective("remainingTime")

                    if (pollResultsObjective != null) {
                        scoreboard.removeObjective(pollResultsObjective)
                    }
                    if (remainingTimeObjective != null) {
                        scoreboard.removeObjective(remainingTimeObjective)
                    }
                    source.sendFeedback(Text.literal("Poll objectives and scoreboard cleared!").formatted(Formatting.GREEN), false)
                    1
                }
            )
            .then(literal("vote")
                .then(argument("option", integer(1))
                    .executes { context ->
                        val option = getInteger(context, "option")
                        val source = context.source
                        val poll = activePolls[source.server.name]
                        if (poll != null) {
                            if (poll.vote(source.player as ServerPlayerEntity, option)) {
                                source.sendFeedback(Text.literal("Vote registered!").formatted(Formatting.GREEN), false)
                            } else {
                                source.sendError(Text.literal("Invalid option/Already casted your vote!").formatted(Formatting.RED))
                            }
                        } else {
                            source.sendError(Text.literal("No active poll found!").formatted(Formatting.RED))
                        }
                        1
                    }
                )
            )
        )
    }
}
