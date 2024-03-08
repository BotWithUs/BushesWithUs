package net.botwithus

import net.botwithus.rs3.imgui.ImGui
import net.botwithus.rs3.imgui.ImGuiWindowFlag
import net.botwithus.rs3.script.ScriptConsole
import net.botwithus.rs3.script.ScriptGraphicsContext
import kotlin.String

class BushesWithUsGraphicsContext(
    private val script: BushesWithUs,
    console: ScriptConsole
) : ScriptGraphicsContext (console) {


    override fun drawSettings() {
        super.drawSettings()
        if (ImGui.Begin("BushesWithUs", ImGuiWindowFlag.None.value)) {
            if (ImGui.BeginTabBar("Bar", ImGuiWindowFlag.None.value)) {
                if (ImGui.BeginTabItem("Settings", ImGuiWindowFlag.None.value)) {
                    ImGui.Text("Script state: ${script.botState}")
                    if (ImGui.Button("Start")) {
                        script.botState = BushesWithUs.BotState.SKILLING
                    }
                    ImGui.SameLine()
                    if (ImGui.Button("Stop")) {
                        script.botState = BushesWithUs.BotState.IDLE
                    }
                    ImGui.Combo("Bush Type", script.bushType, *BushesWithUs.Bush.toStringArray())
                    ImGui.EndTabItem()
                    script.useCompost = ImGui.Checkbox("Use Compost", script.useCompost)
                }
                if (ImGui.BeginTabItem("Stats", ImGuiWindowFlag.None.value)) {
                    val elapsedTime: Long = System.currentTimeMillis() - script.startTime

                    // Convert milliseconds to hours, minutes, and seconds

                    // Convert milliseconds to hours, minutes, and seconds
                    val seconds = elapsedTime / 1000 % 60
                    val minutes = elapsedTime / (1000 * 60) % 60
                    val hours = elapsedTime / (1000 * 60 * 60) % 24
                    ImGui.Text(
                        "Runtime: %02d:%02d:%02d%n",
                        hours, minutes, seconds
                    )
                    ImGui.Text("TTL: %s", script.ttl)
                    ImGui.Text(String.format("Levels Gained: %d | Xp gained %,d", script.levelsGained, script.xpGained))
                    ImGui.Text(String.format("Levels/hr          %,d | Xp/hr %,d", script.levelsPerHour, script.xpPerHour))
                    ImGui.Separator()
                    ImGui.Text(String.format("Roses:     %d | Irises:     %d | Hydrangeas:     %d | Hollyhocks:     %d", script.rosesCollected, script.irisCollected, script.hydrangeaCollected, script.hollyhockCollected))
                    ImGui.Text(String.format("Roses/hr: %,d | Irises/hr: %,d | Hydrangeas/hr: %,d | Hollyhocks/hr: %,d", script.rosesPerHour, script.irisPerHour, script.hydrangeaPerHour, script.hollyhockPerHour))
                    ImGui.Separator()
                    ImGui.Text(String.format("Golden roses: %d | Golden roses/hr: %,d", script.goldenRosesCollected, script.goldenRosesPerHour))
                    ImGui.Text(String.format("Het's pieces:   %d | Het's pieces/hr: %,d", script.piecesOfHet, script.hetPiecesPerHour))
                    ImGui.Separator()
                    ImGui.Text(String.format("Gas dispersed:   %d | Gas dispersed/hr: %,d", script.gasDispersed, script.gasPerHour))
                    ImGui.Text(String.format("Scarabs shoo'd: %d | Scarabs shoo'd/hr: %,d", script.scarabsShooed, script.scarabsPerHour))
                    ImGui.EndTabItem()
                }
                ImGui.EndTabBar()
            }
            ImGui.End()
        }
    }

    override fun drawOverlay() {
        super.drawOverlay()
    }

}