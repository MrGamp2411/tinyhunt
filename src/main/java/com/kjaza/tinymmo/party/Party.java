package com.kjaza.tinymmo.party;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Party {
    public final UUID owner;
    public final Set<UUID> members;

    public Party(UUID owner) {
        this.owner = owner;
        this.members = new HashSet<>();
        this.members.add(owner);
    }

    public Set<UUID> membersView() {
        return Collections.unmodifiableSet(members);
    }
}
