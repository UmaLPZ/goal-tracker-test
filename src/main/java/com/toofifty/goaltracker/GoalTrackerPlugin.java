package com.toofifty.goaltracker;

import com.google.inject.Provides;
import com.toofifty.goaltracker.goal.ItemTask;
import com.toofifty.goaltracker.goal.QuestTask;
import com.toofifty.goaltracker.goal.SkillLevelTask;
import com.toofifty.goaltracker.goal.SkillXpTask;
import com.toofifty.goaltracker.goal.NpcKillTask;
import com.toofifty.goaltracker.goal.Task;
import com.toofifty.goaltracker.goal.TaskType;
import com.toofifty.goaltracker.ui.GoalTrackerPanel;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerPriceType;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.chatbox.ChatboxItemSearch;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.NavigationButton;
import org.apache.commons.text.similarity.JaroWinklerDistance;

@Slf4j
@PluginDescriptor(name = "Goal Tracker", description = "Keep track of your goals and complete them automatically")
public class GoalTrackerPlugin extends Plugin
{
    private static final JaroWinklerDistance DISTANCE = new JaroWinklerDistance();
    @Inject
    private LootTrackerPlugin lootTrackerPlugin;


    @Getter
    @Inject
    private Client client;

    @Getter
    @Inject
    private SkillIconManager skillIconManager;

    @Getter
    @Inject
    private ItemManager itemManager;

    @Getter
    @Inject
    private ChatboxItemSearch itemSearch;

    @Getter
    @Inject
    private NpcManager npcManager;

    @Getter
    @Inject
    private NpcSearch npcSearch;

    @Inject
    private ClientToolbar clientToolbar;

    @Getter
    @Inject
    private ClientThread clientThread;

    @Getter
    @Inject
    private ItemCache itemCache;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Getter
    @Inject
    private GoalTrackerConfig config;

    @Getter
    private GoalManager goalManager;

    private NavigationButton uiNavigationButton;

    private GoalTrackerPanel goalTrackerPanel;

    @Setter
    private boolean validateAll = true;

    @Provides
    GoalTrackerConfig getGoalTrackerConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GoalTrackerConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        // Get all incomplete NPC kill tasks.
        List<NpcKillTask> npcKillTasks = goalManager.getAllIncompleteTasksOfType(TaskType.NPC_KILL);

        for (NpcKillTask task : npcKillTasks) {
            // Get the kill count for the NPC from the Loot Tracker plugin.
            int killCount = lootTrackerPlugin.getKillCount(task.getNpcId());

            // Update the task's kill count.
            task.setKilled(killCount);

            // If the kill count is met, notify the task and refresh the UI.
            if (task.getKilled() >= task.getKillCount()) {
                notifyTask(task);
                TaskUIStatusManager.getInstance().refresh(task);
            }
        }
    }

    @Override
    protected void startUp()
    {
        goalManager = new GoalManager(this);
        goalManager.load();

        itemCache.load();

        final BufferedImage icon = itemManager.getImage(ItemID.TODO_LIST);
        goalTrackerPanel = new GoalTrackerPanel(this);

        uiNavigationButton = NavigationButton.builder()
            .tooltip("Goal Tracker")
            .icon(icon)
            .priority(7)
            .panel(goalTrackerPanel)
            .build();

        clientToolbar.addNavigation(uiNavigationButton);
    }

    @Override
    protected void shutDown()
    {
        goalManager.save();
        itemCache.save();

        clientToolbar.removeNavigation(uiNavigationButton);
    }

    @Subscribe
    public void onSessionOpen(SessionOpen event)
    {
        goalManager.load();
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        List<SkillLevelTask> skillLevelTasks = goalManager.getAllIncompleteTasksOfType(
            TaskType.SKILL_LEVEL);
        for (SkillLevelTask task : skillLevelTasks) {
            if (event.getSkill() == task.getSkill() && event.getLevel() >= task.getLevel()) {
                notifyTask(task);
                TaskUIStatusManager.getInstance().refresh(task);
            }
        }

        List<SkillXpTask> skillXpTasks = goalManager.getAllIncompleteTasksOfType(
            TaskType.SKILL_XP);
        for (SkillXpTask task : skillXpTasks) {
            if (event.getSkill() == task.getSkill() && event.getXp() >= task.getXp()) {
                notifyTask(task);
                TaskUIStatusManager.getInstance().refresh(task);
            }
        }
    }

    public void trackNpcLoot(String npcName) {
        int killCount = 0;
        int lootValue = 0;

        // Iterate through the loot received from the Loot Tracker Plugin
        for (LootReceived loot : lootTrackerPlugin.getAllLoot()) {
            if (loot.getName().equals(npcName)) {
                killCount += loot.getAmount();

                // Calculate loot value based on the GRAND_EXCHANGE price
                for (ItemStack item : loot.getItems()) {
                    lootValue += item.getId().getPrice(LootTrackerPriceType.GRAND_EXCHANGE) * item.getQuantity();
                }
            }
        }

        // Get the NPC kill goal from the goal manager
        NpcKillGoal goal = goalManager.getNpcKillGoal(npcName);

        // If the goal doesn't exist yet, create a new one
        if (goal == null) {
            goal = new NpcKillGoal(npcName, killCount, lootValue);
            goalManager.addGoal(goal);
        } else {
            // If the goal already exists, update its kill count and loot value
            goal.setKillCount(killCount);
            goal.setLootValue(lootValue);
        }

        // Notify the goal manager that a goal has been updated
        goalManager.goalUpdated(goal);
    }

    public void notifyTask(Task task)
    {
        if (client.getGameState() != GameState.LOGGED_IN || task.hasBeenNotified()) {
            return;
        }

        System.out.println(
            "Notify: " + "[Goal Tracker] You have completed a task: " + task.toString() + "!");

        String message = "[Goal Tracker] You have completed a task: " + task.toString() + "!";
        String formattedMessage = new ChatMessageBuilder().append(
            ColorScheme.PROGRESS_COMPLETE_COLOR, message).build();
        chatMessageManager.queue(QueuedMessage.builder()
            .type(ChatMessageType.CONSOLE)
            .name("Goal Tracker")
            .runeLiteFormattedMessage(
                formattedMessage)
            .build());

        task.hasBeenNotified(true);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        // redo the login check on the next game tick
        validateAll = true;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!validateAll) {
            return;
        }
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        validateAll = false;
        // perform a full refresh just once on login
        // onGameStateChanged reports incorrect quest statuses,
        // so this need to be done in this subscriber
        goalTrackerPanel.refresh();
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        // attempt to refresh
        if (event.getType() == ChatMessageType.GAMEMESSAGE && event.getMessage()
            .contains("Quest complete")) {

            List<QuestTask> questTasks = goalManager.getAllIncompleteTasksOfType(
                TaskType.QUEST);
            for (QuestTask task : questTasks) {
                if (task.check().isCompleted()) {
                    notifyTask(task);
                    TaskUIStatusManager.getInstance().refresh(task);
                }
            }
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        itemCache.update(event.getContainerId(), event.getItemContainer().getItems());

        List<ItemTask> itemTasks = goalManager.getAllIncompleteTasksOfType(TaskType.ITEM);
        for (ItemTask task : itemTasks) {
            if (task.getResult().isCompleted()) {
                continue;
            }

            if (task.check().isCompleted()) {
                notifyTask(task);
            }

            // always refresh item tasks, since the acquired
            // count could have changed
            TaskUIStatusManager.getInstance().refresh(task);
        }
    }
}
