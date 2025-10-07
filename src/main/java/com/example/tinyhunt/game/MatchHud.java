package com.example.tinyhunt.game;

import com.example.tinyhunt.TinyHuntPlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

/**
 * Handles the sidebar scoreboard and bossbar that track an active TinyHunt match.
 */
public final class MatchHud {

    private final TinyHuntPlugin plugin;
    private final BossBar bossBar;
    private final Map<UUID, Scoreboard> sidebars = new HashMap<>();

    public MatchHud(TinyHuntPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.bossBar = Bukkit.createBossBar("TinyHunt", BarColor.RED, BarStyle.SOLID);
        this.bossBar.setVisible(false);
    }

    public void start(Collection<UUID> participants) {
        bossBar.setVisible(true);
        syncParticipants(participants);
    }

    public void stop() {
        bossBar.removeAll();
        bossBar.setVisible(false);
        for (UUID uuid : new ArrayList<>(sidebars.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        sidebars.clear();
    }

    public void update(Collection<UUID> participants, HudSnapshot snapshot) {
        syncParticipants(participants);
        bossBar.removeAll();
        bossBar.setTitle(snapshot.bossBarTitle());
        bossBar.setProgress(snapshot.progress());
        for (UUID uuid : new HashSet<>(sidebars.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                sidebars.remove(uuid);
                continue;
            }
            List<String> lines = buildLines(snapshot);
            Scoreboard scoreboard = sidebars.get(uuid);
            if (scoreboard != null) {
                renderSidebar(scoreboard, lines, snapshot.scoreboardTitle());
                bossBar.addPlayer(player);
            }
        }
    }

    private void syncParticipants(Collection<UUID> participants) {
        Set<UUID> snapshot = new HashSet<>(participants);
        for (UUID uuid : new HashSet<>(sidebars.keySet())) {
            if (!snapshot.contains(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    bossBar.removePlayer(player);
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
                sidebars.remove(uuid);
            }
        }
        for (UUID uuid : snapshot) {
            if (sidebars.containsKey(uuid)) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager == null) {
                continue;
            }
            Scoreboard scoreboard = manager.getNewScoreboard();
            sidebars.put(uuid, scoreboard);
            player.setScoreboard(scoreboard);
        }
    }

    private void renderSidebar(Scoreboard scoreboard, List<String> lines, String title) {
        Objective objective = scoreboard.getObjective("tinyhunt");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("tinyhunt", "dummy", ChatColor.GOLD + title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.setDisplayName(ChatColor.GOLD + title);
        }
        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }
        int score = lines.size();
        for (String line : lines) {
            objective.getScore(line).setScore(score--);
        }
    }

    private List<String> buildLines(HudSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.YELLOW + "Tempo: " + snapshot.formattedTime());
        lines.add(ChatColor.GREEN + "Fuggitivi: " + snapshot.runners());
        lines.add(ChatColor.RED + "Cacciatori: " + snapshot.hunters());
        if (snapshot.nextRevealSeconds() >= 0) {
            String reveal = snapshot.nextRevealSeconds() == 0 ? "In corso" : snapshot.nextRevealSeconds() + "s";
            lines.add(ChatColor.LIGHT_PURPLE + "Ping: " + reveal);
        }
        if (snapshot.extraLine() != null && !snapshot.extraLine().isBlank()) {
            lines.add(ChatColor.GRAY + snapshot.extraLine());
        }
        return lines;
    }

    /**
     * Immutable data describing the HUD state for the current tick.
     */
    public record HudSnapshot(String bossBarTitle, String scoreboardTitle, double progress, String formattedTime,
            int runners, int hunters, long nextRevealSeconds, String extraLine) {
    }
}
