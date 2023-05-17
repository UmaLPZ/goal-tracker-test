package com.toofifty.goaltracker.goal.factory;

import com.google.gson.JsonObject;
import com.toofifty.goaltracker.GoalTrackerPlugin;
import com.toofifty.goaltracker.goal.Goal;
import com.toofifty.goaltracker.goal.NpcKillTask;

public class NpcKillTaskFactory extends TaskFactory {
    public NpcKillTaskFactory(GoalTrackerPlugin plugin, Goal goal) {
        super(plugin, goal);
    }

    @Override
    protected NpcKillTask createObjectFromJson(JsonObject json) {
        return create(
                json.get("npc_id").getAsInt(),
                json.get("npc_name").getAsString(),
                json.get("kill_count").getAsInt(),
                json.get("kills_acquired").getAsInt());
    }

    public NpcKillTask create(int npcId, String npcName, int killCount, int killsAcquired) {
        NpcKillTask task = new NpcKillTask(goal); // make sure you have the right constructor and it fits your design

        task.setNpcId(npcId);
        task.setNpcName(npcName);
        task.setKillCount(killCount);
        task.setKillsAcquired(killsAcquired);
        return task;
    }

    public NpcKillTask create(int npcId, String npcName, int killCount) {
        return create(npcId, npcName, killCount, 0);
    }
}
