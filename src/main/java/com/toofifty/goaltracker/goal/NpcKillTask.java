package com.toofifty.goaltracker.goal;

import net.runelite.api.Client;
import com.toofifty.goaltracker.GoalTrackerPlugin;
import java.awt.image.BufferedImage;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

public class NpcKillTask extends Task {
    @Setter
    @Getter
    private int npcId;

    @Setter
    @Getter
    private String npcName;

    @Setter
    @Getter
    private int killCount;

    @Setter
    @Getter
    private int killsAcquired;

    // Add a field for total loot value
    @Setter
    @Getter
    private long totalLootValue;

    // Add a field for target loot value
    @Setter
    @Getter
    private long targetLootValue;

    public NpcKillTask(Goal goal) {
        super(goal);
    }

    @Override
    public String toString() {
        return npcName + " " + killsAcquired + "/" + killCount;
    }

    @Override
    public TaskType getType() {
        return TaskType.NPC_KILL;
    }

    @Override
    protected JsonObject addSerializedProperties(JsonObject json) {
        json.addProperty("npc_id", npcId);
        json.addProperty("npc_name", npcName);
        json.addProperty("kill_count", killCount);
        json.addProperty("kills_acquired", killsAcquired);
        json.addProperty("total_loot_value", totalLootValue);  // Add total loot value to JSON
        json.addProperty("target_loot_value", targetLootValue);  // Add target loot value to JSON
        return json;
    }

    // You'll need to provide an implementation for this method.
    // It might be similar to your other task classes.
    @Override
    public BufferedImage getIcon() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public TaskStatus check() {
        if (killsAcquired >= killCount || totalLootValue >= targetLootValue) { // Check if loot value goal is also met
            return TaskStatus.COMPLETED;
        } else if (killsAcquired > 0 || totalLootValue > 0) { // Check if progress towards loot value goal is being made
            return TaskStatus.IN_PROGRESS;
        } else {
            return TaskStatus.NOT_STARTED;
        }
    }
}
