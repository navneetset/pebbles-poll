import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.scoreboard.Scoreboard
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class Poll(private val question: String) {
    var duration: Int = 0
    private val options: MutableList<String> = mutableListOf()
    private val votes: MutableMap<Int, Int> = mutableMapOf()
    private var scoreboard: ScoreboardObjective? = null
    private lateinit var pollResultsObjective: ScoreboardObjective
    private val votedPlayers: MutableSet<UUID> = mutableSetOf()

    fun addOption(option: String) {
        options.add(option)
    }

    private fun updateRemainingTime(scoreboard: Scoreboard, minutes: Int, seconds: Int) {
        val score = scoreboard.getPlayerScore("Time", pollResultsObjective)
        score.score = seconds + minutes * 60
    }



    fun start(source: ServerCommandSource, onPollEnd: (ServerCommandSource) -> Unit) {
        // Broadcast the poll question and options to players
        initScoreboard(source)
        val question = Text.literal("$question").formatted(Formatting.GREEN)
        broadcast(source.server, Text.literal("Poll started! Question:").formatted(Formatting.GREEN))
        broadcast(source.server, question.formatted(Formatting.YELLOW))
        options.forEachIndexed { index, option ->
            val optionText = Text.literal("${index + 1}. $option").formatted(Formatting.YELLOW).formatted(Formatting.UNDERLINE)
            val style = Style.EMPTY
                .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pebblespoll vote ${index + 1}"))
                .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to vote for $option")))

            optionText.style = style.withColor(Formatting.YELLOW)
            broadcast(source.server, optionText)
            broadcast(source.server, Text.literal(" ").formatted(Formatting.YELLOW))
        }
        broadcast(source.server, Text.literal("Click on an option or type /pebblespoll vote <option_number> to vote!").formatted(Formatting.BLUE))

        // Start the timer
        GlobalScope.launch {
            val scoreboard = source.server.scoreboard
            for (i in duration * 60 downTo 0) {
                val minutes = i / 60
                val seconds = i % 60
                updateRemainingTime(scoreboard, minutes, seconds)
                delay(1000L) // Wait for 1 second
            }
            displayResults(source)
            onPollEnd(source)
        }

    }


    fun vote(player: ServerPlayerEntity, option: Int): Boolean {
        if (player.uuid in votedPlayers) {
            return false
        }

        if (option in 1..options.size) {
            votes[option] = votes.getOrDefault(option, 0) + 1

            // Update the scoreboard
            val scoreboard = player.server.scoreboard
            val score = scoreboard.getPlayerScore(options[option - 1], pollResultsObjective)
            score.incrementScore(1)

            // Add the player to the votedPlayers set
            votedPlayers.add(player.uuid)

            return true
        }
        return false
    }



    private fun initScoreboard(source: ServerCommandSource) {
        val scoreboard = source.server.scoreboard

        // Create the combined Poll objective
        val pollName = "Poll"
        val pollDisplayName = Text.of("Poll")
        val pollCriteria = ScoreboardCriterion.DUMMY
        pollResultsObjective = createOrUpdateObjective(scoreboard, pollName, pollDisplayName, pollCriteria, 1)

        // Initialize poll results with the options and their current vote count
        initializePollResults(scoreboard)

        // Set the display order for the objectives
        scoreboard.setObjectiveSlot(1, pollResultsObjective)
    }

    private fun initializePollResults(scoreboard: Scoreboard) {
        val time = scoreboard.getPlayerScore("Time", pollResultsObjective)
        time.score = duration * 60
        options.forEachIndexed { index, option ->
            val score = scoreboard.getPlayerScore(option, pollResultsObjective)
            score.score = 0
        }
    }


    private fun createOrUpdateObjective(scoreboard: Scoreboard, name: String, displayName: Text, criteria: ScoreboardCriterion, slot: Int): ScoreboardObjective {
        val existingObjective = scoreboard.getObjective(name)
        return if (existingObjective == null) {
            val newObjective = scoreboard.addObjective(name, criteria, displayName, ScoreboardCriterion.RenderType.INTEGER)
            scoreboard.setObjectiveSlot(slot, newObjective)
            newObjective
        } else {
            existingObjective.setDisplayName(displayName)
            scoreboard.setObjectiveSlot(slot, existingObjective)
            existingObjective
        }
    }

    fun displayResults(source: ServerCommandSource) {
        broadcast(source.server, Text.literal("Poll ended! Results for '$question':").formatted(Formatting.GREEN))
        if (scoreboard != null) {
            source.server.scoreboard.removeObjective(scoreboard)
        }
        if (votes.isEmpty()) {
            broadcast(source.server, Text.literal("No votes were cast."))
        } else {
            val totalVotes = votes.values.sum()
            options.forEachIndexed { index, option ->
                val optionVotes = votes.getOrDefault(index + 1, 0)
                val percentage = (optionVotes.toDouble() / totalVotes.toDouble()) * 100.0
                broadcast(source.server, Text.literal("Option ${index + 1} - $option: $optionVotes votes (${String.format("%.1f", percentage)}%)"))
            }
        }
    }


    fun displayQuestionAndOptions(source: ServerCommandSource) {
        val question = Text.literal("$question").formatted(Formatting.GREEN)
        broadcast(source.server, Text.literal("Current Poll:").formatted(Formatting.GREEN))
        broadcast(source.server, question.formatted(Formatting.YELLOW))
        options.forEachIndexed { index, option ->
            val optionText = Text.literal("${index + 1}. $option").formatted(Formatting.YELLOW).formatted(Formatting.UNDERLINE)
            val style = Style.EMPTY
                .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pebblespoll vote ${index + 1}"))
                .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to vote for $option")))

            optionText.style = style.withColor(Formatting.YELLOW)
            broadcast(source.server, optionText)
            broadcast(source.server, Text.literal(" ").formatted(Formatting.YELLOW))
        }
        broadcast(source.server, Text.literal("Click on an option or type /pebblespoll vote <option_number> to vote!").formatted(Formatting.YELLOW))
    }

    private fun broadcast(server: MinecraftServer, message: Text?) {
        for (player in server.playerManager.playerList) {
            player.sendMessage(message, false)
        }
    }
}
