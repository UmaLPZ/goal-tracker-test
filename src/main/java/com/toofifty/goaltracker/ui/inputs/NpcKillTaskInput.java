package com.toofifty.goaltracker.ui.inputs;

import com.toofifty.goaltracker.GoalTrackerPlugin;
import com.toofifty.goaltracker.goal.Goal;
import com.toofifty.goaltracker.goal.NpcKillTask;
import com.toofifty.goaltracker.goal.factory.NpcKillTaskFactory;
import com.toofifty.goaltracker.ui.SimpleDocumentListener;
import com.toofifty.goaltracker.ui.TextButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.api.NPCComposition;
import net.runelite.client.game.NPCManager;

public class NpcKillTaskInput extends TaskInput {
    private final Goal goal;
    private final NPCManager npcManager;
    private final ClientThread clientThread;

    private final FlatTextField killCountField = new FlatTextField();
    private final TextButton searchNpcButton = new TextButton("Search...");
    private final JLabel selectedNpcLabel = new JLabel();
    private final JPanel selectedNpcPanel = new JPanel(new BorderLayout());

    private NPCComposition selectedNpc;
    private final TextButton clearNpcButton = new TextButton("X")
            .setMainColor(ColorScheme.PROGRESS_ERROR_COLOR)
            .onClick((e) -> clearSelectedNpc());

    public NpcKillTaskInput(GoalTrackerPlugin plugin, Goal goal) {
        super(plugin, "NPC Kill");
        this.goal = goal;
        this.npcManager = plugin.getNpcManager();
        this.clientThread = plugin.getClientThread();

        searchNpcButton.onClick(e -> {
            if (plugin.getClient().getGameState() != GameState.LOGGED_IN) {
                JOptionPane.showMessageDialog(this,
                        "You must be logged in to choose NPCs",
                        "UwU",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            plugin.getNpcSearch()
                    .tooltipText("Choose an NPC")
                    .onItemSelected(this::setSelectedNpc)
                    .build();
        });
        getInputRow().add(searchNpcButton, BorderLayout.WEST);

        killCountField.setBorder(new EmptyBorder(0, 8, 0, 8));
        killCountField.getTextField().setHorizontalAlignment(SwingConstants.RIGHT);
        killCountField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        killCountField.setPreferredSize(new Dimension(92, PREFERRED_INPUT_HEIGHT));

        getInputRow().add(killCountField, BorderLayout.CENTER);

        selectedNpcPanel.setBorder(new EmptyBorder(0, 8, 0, 8));
        selectedNpcPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        selectedNpcPanel.add(selectedNpcLabel, BorderLayout.CENTER);
        selectedNpcPanel.add(clearNpcButton, BorderLayout.EAST);
    }

    private void setSelectedNpc(int rawId) {
        clientThread.invokeLater(() -> {
            selectedNpc = npcManager.getNpcComposition(rawId);
            selectedNpcLabel.setText(selectedNpc.getName());

            getInputRow().remove(searchNpcButton);
            getInputRow().add(selectedNpcPanel, BorderLayout.WEST);

            revalidate();
            repaint();
        });
    }

    @Override
    protected void onSubmit() {
        if (selectedNpc == null || killCountField.getText().isEmpty()) {
            return;
        }

        NpcKillTask task = new NpcKillTaskFactory(plugin, goal)
                .create(selectedNpc.getId(), selectedNpc.getName(),
                        Integer.parseInt(killCountField.getText()));
        goal.add(task);

        getUpdater().run();
        reset();
    }

    private void clearSelectedNpc() {
        selectedNpc = null;

        getInputRow().remove(selectedNpcPanel);
        getInputRow().add(searchNpcButton, BorderLayout.WEST);

        revalidate();
        repaint();
    }

    @Override
    protected void reset() {
        clearSelectedNpc();
        killCountField.setText("");
    }
}

