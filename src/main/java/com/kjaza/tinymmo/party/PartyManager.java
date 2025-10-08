package com.kjaza.tinymmo.party;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyManager {
    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, UUID> playerToParty = new HashMap<>();

    public Party createParty(UUID owner) {
        Party party = new Party(owner);
        parties.put(owner, party);
        playerToParty.put(owner, owner);
        return party;
    }

    public void disbandParty(UUID owner) {
        Party party = parties.remove(owner);
        if (party == null) {
            return;
        }
        for (UUID member : party.members) {
            playerToParty.remove(member);
        }
    }

    public void addMember(UUID owner, UUID member) {
        Party party = parties.get(owner);
        if (party == null) {
            party = createParty(owner);
        }
        party.members.add(member);
        playerToParty.put(member, owner);
    }

    public void removeMember(UUID member) {
        UUID owner = playerToParty.remove(member);
        if (owner == null) {
            return;
        }
        Party party = parties.get(owner);
        if (party == null) {
            return;
        }
        party.members.remove(member);
        if (party.members.isEmpty()) {
            parties.remove(owner);
        }
    }

    public Party getPartyOf(UUID player) {
        UUID owner = playerToParty.get(player);
        if (owner == null) {
            return null;
        }
        return parties.get(owner);
    }
}
