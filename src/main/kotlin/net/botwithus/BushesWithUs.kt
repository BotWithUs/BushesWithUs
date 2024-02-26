package net.botwithus

import net.botwithus.internal.scripts.ScriptDefinition
import net.botwithus.rs3.events.impl.InventoryUpdateEvent
import net.botwithus.rs3.events.impl.SkillUpdateEvent
import net.botwithus.rs3.game.Client
import net.botwithus.rs3.game.queries.builders.animations.SpotAnimationQuery
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery
import net.botwithus.rs3.game.scene.entities.animation.SpotAnimation
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer
import net.botwithus.rs3.game.scene.entities.`object`.SceneObject
import net.botwithus.rs3.game.skills.Skills
import net.botwithus.rs3.imgui.NativeInteger
import net.botwithus.rs3.script.Execution
import net.botwithus.rs3.script.LoopingScript
import net.botwithus.rs3.script.config.ScriptConfig
import net.botwithus.rs3.util.Regex
import java.util.*
import java.util.regex.Pattern

class BushesWithUs(
    name: String,
    scriptConfig: ScriptConfig,
    scriptDefinition: ScriptDefinition
) : LoopingScript (name, scriptConfig, scriptDefinition) {

    //Declare types explicitly, Kotlin will use the type you specify.
    private val random: Random = Random()
    var botState: BotState = BotState.IDLE
    var bushType: NativeInteger = NativeInteger(0)
    var xpGained: Int = 0
    var gasDispersed: Int = 0
    var scarabsShooed: Int = 0
    var rosesCollected: Int = 0
    var irisCollected: Int = 0
    var hydrangeaCollected: Int = 0
    var hollyhockCollected: Int = 0
    var goldenRosesCollected: Int = 0
    var piecesOfHet: Int = 0
    var levelsGained = 0
    var startTime = System.currentTimeMillis()

    //Declare types inherently, Kotlin will assume integer here.
    var xpPerHour = 0
    var gasPerHour = 0
    var scarabsPerHour = 0
    var rosesPerHour = 0
    var irisPerHour = 0
    var hydrangeaPerHour = 0
    var hollyhockPerHour = 0
    var goldenRosesPerHour = 0
    var hetPiecesPerHour = 0
    var levelsPerHour = 0

    private var animDeadCount = 0

    var ttl: String = "You ain't gonna level sitting around!"

    enum class BotState {
        //define your bot states here
        IDLE,
        SKILLING,
        //etc..
    }

    enum class Bush {
        Rose,
        Iris,
        Hydrangea,
        Hollyhock;

        companion object {
            fun toStringArray(): Array<String> {
                return entries.map { it.name }.toTypedArray()
            }
        }
    }

    override fun initialize(): Boolean {
        super.initialize()
        // Set the script graphics context to our custom one
        this.sgc = BushesWithUsGraphicsContext(this, console)

        // Use the event bus system to subscribe to SkillUpdateEvent
        // This code will fire every time a skill receives an update event from the server (level change, xp gain, etc)
        subscribe(SkillUpdateEvent::class.java) {
            xpGained += it.experience - it.oldExperience
            if (it.actualLevel - it.oldActualLevel > 0)
                levelsGained += it.actualLevel - it.oldActualLevel
        }

        // Use the event bus system to subscribe to InventoryUpdateEvent
        // This code will fire every time any inventory receives an update event from the server (item added, stack size changed, etc)
        subscribe(InventoryUpdateEvent::class.java) {
            // Update our statistics based on the item that was updated in the inventory.
            if (it.inventoryId == 93) {
                // Make sure we only do it for the backpack inventory, 93.
                // Otherwise, these stats could update when flowers are deposited to the bank for example (Inventory 95)
                when (it.newItem.name) {
                    "Roses" -> {
                        rosesCollected += it.newItem.stackSize - it.oldItem.stackSize
                    }
                    "Irises" -> {
                        irisCollected += it.newItem.stackSize - it.oldItem.stackSize
                    }
                    "Hydrangeas" -> {
                        hydrangeaCollected += it.newItem.stackSize - it.oldItem.stackSize
                    }
                    "Hollyhocks" -> {
                        hollyhockCollected += it.newItem.stackSize - it.oldItem.stackSize
                    }
                    "Golden roses" -> {
                        goldenRosesCollected += it.newItem.stackSize - it.oldItem.stackSize
                    }
                    "Piece of Het" -> {
                        piecesOfHet = it.newItem.stackSize - it.oldItem.stackSize
                    }
                }
            }
        }
        loadConfiguration()
        return true
    }

    // Save values that need to persist to the script configuration properties.
     fun saveConfiguration() {
        configuration.addProperty("bushType", bushType.get().toString())
        configuration.addProperty("botState", botState.name)
        configuration.save()
    }

    // Attempt to load the persistent properties from the script configuration.
    fun loadConfiguration() {
        try {
            bushType.set(configuration.getProperty("bushType").toInt())
            botState = BotState.valueOf(configuration.getProperty("botState"))
            println("Persistent data loaded successfully.")
            println("Bot state: $botState")
            println("Bush type: ${Bush.entries[bushType.get()].name}")
        } catch (e: Exception) {
            println("Failed to load persistent properties. Using defaults.")
            e.printStackTrace()
        }

    }

    override fun onLoop() {
        // Fetch the local player from the game client
        val player = Client.getLocalPlayer()

        // If the player is null, not logged in, or our bot state is idle, we don't want to do anything.
        // Return a random delay and try again in a bit.
        if (Client.getGameState() != Client.GameState.LOGGED_IN || player == null || botState == BotState.IDLE) {
            Execution.delay(random.nextLong(2500,5500))
            return
        }
        // At this point, we have our player, we're logged in, and our script is in a state it should be doing something.

        // Update our statistics based on values that may have changed since last onLoop iteration
        // For example, our inventory update event could have fired, and we need to update the numbers ImGui displays.
        updateStatistics()

        // Save current state
        saveConfiguration()

        // Handle the possible bot states
        when (botState) {
            BotState.SKILLING -> {
                // Delay  by the randomized value returned from handleSkilling, then return from onLoop
                Execution.delay(handleSkilling(player))
                return
            }
            BotState.IDLE -> {
                // Delay and do nothing, we're in the idle state.
                Execution.delay(random.nextLong(2500,5500))
                return
            }
        }
    }

    private fun handleSkilling(player: LocalPlayer): Long {
        // Create a bush name pattern from our Bush enum and the user's UI selection.
        val bushName: Pattern = Regex.getPatternForContainsString(Bush.entries[bushType.get()].name)

        //If we're moving, we're not ready to do anything. Return from function and try again in a bit.
        if (player.isMoving)
            return random.nextLong(650,1435)

        //As a priority, Handle the scarab disturbance that slows xp gain.
        val pestyScarab: Npc? = NpcQuery.newQuery().name("Pesty scarab").option("Shoo").results().nearest()
        if (pestyScarab != null) {
            val success: Boolean = pestyScarab.interact("Shoo")
            println("Interacted pesty scarab: $success")
            if (success) {
                scarabsShooed++
                return random.nextLong(1875, 3245)
            }
        }

        //Next, handle nutritous gas to boost xp.
        val nutritiousGas: SpotAnimation? = SpotAnimationQuery.newQuery().ids(7620).results().nearest()
        if (nutritiousGas != null) {
            val nearestResult = SceneObjectQuery.newQuery().name(bushName).option("Cultivate").results()?.nearest()
            val success: Boolean = nearestResult?.interact("Cultivate") ?: false
            println("Interacted nutritious gas: $success")
            if (success) {
                gasDispersed++
                return random.nextLong(1245, 2576)
            }
        }

        //Then, if the bush is fully grown, it will have option "Harvest" and we should prioritize it.
        val grownBush: SceneObject? = SceneObjectQuery.newQuery().name(bushName).option("Harvest").results().nearest()
        if (grownBush != null) {
            val success: Boolean = grownBush.interact("Harvest")
            println("Interacted grown bush: $success")
            if (success)
                return random.nextLong(305,1254)
        }

        // The animation for bush harvesting is very quick, and loops frequently.
        // This mean there are a lot of downtimes in the animation loop where the game will consider your player not animating, even though you're still using the bush.
        // This can result in frequently clicking the bush while still interacting it and only when not animating, not exactly a normal human behavior.
        // To address this, we create an Int that keeps track of our attempts to find the player is no longer animating.
        if (player.animationId != -1) {
            // If we find the player not animating, we reset the counter and try again later.
            animDeadCount = 0
            return random.nextLong(650, 1435)
        } else {
            // If the player is found to be not animating, lets make sure its been for a while.
            if (animDeadCount > 2) {
                // If our counter is greater than two, we have now 3 times (animDeadCount = 0,1,2) found the player not animating.
                // Safe to assume we are no longer interacting the bush at this point
                // reset it back to 0 and continue with the rest of the function code to click the bush.
                animDeadCount = 0
            } else {
                // If our counter is not above two, increment it and return a delay.
                animDeadCount++
                return random.nextLong(350,746)
            }
        }

        //At this point, check if we need to click the bush and do so.
        val bush: SceneObject? = SceneObjectQuery.newQuery().name(bushName).option("Cultivate").results().nearest()
        if (bush != null) {
            //Headbar wasn't going away fast enough when finished with this strategy, so i ditched this for the animDeadCheck.
//            if (player.headbars.none { it.id == 13 }) {
                println("Interacted bush: ${bush.interact("Cultivate")}")
//            } else {
//                println("Cultivating headbar active, no action needed.")
//            }
        } else {
            println("Bush not found.")
        }

        // Wait a while and loop again later.
        return random.nextLong(2345, 6785)
    }

    private fun updateStatistics() {
        val currentTime: Long = (System.currentTimeMillis() - startTime) / 1000

        xpPerHour = Math.round(3600.0 / currentTime * xpGained).toInt()
        gasPerHour = Math.round(3600.0 / currentTime * gasDispersed).toInt()
        scarabsPerHour = Math.round(3600.0 / currentTime * scarabsShooed).toInt()
        rosesPerHour = Math.round(3600.0 / currentTime * rosesCollected).toInt()
        irisPerHour = Math.round(3600.0 / currentTime * irisCollected).toInt()
        hydrangeaPerHour = Math.round(3600.0 / currentTime * hydrangeaCollected).toInt()
        hollyhockPerHour = Math.round(3600.0 / currentTime * hollyhockCollected).toInt()
        goldenRosesPerHour = Math.round(3600.0 / currentTime * goldenRosesCollected).toInt()
        hetPiecesPerHour = Math.round(3600.0 / currentTime * piecesOfHet).toInt()
        levelsPerHour = Math.round(3600.0 / currentTime * levelsGained).toInt()
        if (xpPerHour != 0) {
            val totalSeconds = Skills.FARMING.experienceToNextLevel * 3600 / xpPerHour
            val hours = totalSeconds / 3600
            val minutes = totalSeconds % 3600 / 60
            val seconds = totalSeconds % 60
            ttl = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

}