package com.toofifty.goaltracker;

import net.runelite.api.NPC;
import org.apache.commons.text.similarity.JaroWinklerDistance;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class NpcSearch {

    private final NpcManager npcManager;
    private String searchText;
    private Consumer<NPC> onNpcSelectedAction;

    @Inject
    public NpcSearch(NpcManager npcManager) {
        this.npcManager = npcManager;
    }

    public NpcSearch tooltipText(String text) {
        this.searchText = text;
        return this;
    }

    public NpcSearch onNpcSelected(Consumer<NPC> action) {
        this.onNpcSelectedAction = action;
        return this;
    }

    public void build() {
        if (searchText == null || onNpcSelectedAction == null) {
            throw new IllegalStateException("Search text and onNpcSelected action must be set before calling build()");
        }

        JaroWinklerDistance jwd = new JaroWinklerDistance();
        Collection<NPC> allNpcs = npcManager.getAllNpcs();
        NPC matchingNpc = allNpcs.stream()
                .filter(npc -> jwd.apply(searchText.toLowerCase(), npc.getName().toLowerCase()) > 0.9)
                .findFirst()
                .orElse(null);

        if (matchingNpc != null) {
            onNpcSelectedAction.accept(matchingNpc);
        }
    }

    public Collection<NPC> getAllNpcs() {
        return npcManager.getAllNpcs();
    }

    public NPC getNpcByName(String name) {
        return npcManager.getNpcByName(name);
    }
}
