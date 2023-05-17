package com.toofifty.goaltracker;

import net.runelite.api.NPC;
import net.runelite.api.Client;
import javax.inject.Inject;
import java.util.Collection;

public class NpcManager {

    private final Client client;

    @Inject
    public NpcManager(Client client) {
        this.client = client;
    }

    public Collection<NPC> getAllNpcs() {
        return client.getNpcs();
    }

    public NPC getNpcByName(String name) {
        return client.getNpcs().stream()
                .filter(npc -> name.equals(npc.getName()))
                .findFirst()
                .orElse(null);
    }
}
