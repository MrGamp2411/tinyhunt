package com.example.tinyhunt.game;

import com.example.tinyhunt.TinyHuntPlugin;
import com.example.tinyhunt.model.ConfigLocationUtil;
import com.example.tinyhunt.model.ConfiguredArea;
import com.example.tinyhunt.model.PlayerRole;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Coordinates queue management, game flow, and configuration-backed state.
 */
public final class GameManager {

    private final TinyHuntPlugin plugin;
    private ConfiguredArea lobbyArea;
    private ConfiguredArea arenaArea;
    private final List<Location> arenaSpawns = new ArrayList<>();

    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private final LinkedHashSet<UUID> activePlayers = new LinkedHashSet<>();
    private final Map<UUID, PlayerRole> roles = new HashMap<>();

    private GameState state = GameState.WAITING;
    private BukkitTask countdownTask;
    private int countdownSecondsRemaining;
    private BukkitTask hunterSelectionTask;
    private BukkitTask gameTimerTask;

    private int minPlayers;
    private int maxPlayers;
    private int autoStartSeconds;
    private int hunterDelaySeconds;
    private int gameDurationSeconds;
    private float runnerScale;
    private float hunterScale;
    private Attribute cachedScaleAttribute;
    private boolean scaleAttributeResolved;
    private boolean scaleWarningLogged;

    public GameManager(TinyHuntPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        reloadSettings();
    }

    public void reloadSettings() {
        plugin.reloadConfig();
        minPlayers = Math.max(2, plugin.getConfig().getInt("players.min", 4));
        maxPlayers = Math.max(minPlayers, plugin.getConfig().getInt("players.max", minPlayers));
        autoStartSeconds = Math.max(5, plugin.getConfig().getInt("timers.auto-start-seconds", 120));
        hunterDelaySeconds = Math.max(1, plugin.getConfig().getInt("timers.hunter-selection-seconds", 10));
        gameDurationSeconds = Math.max(30, plugin.getConfig().getInt("timers.game-duration-seconds", 600));
        runnerScale = (float) plugin.getConfig().getDouble("scales.runner", 0.33D);
        hunterScale = (float) plugin.getConfig().getDouble("scales.hunter", 1.0D);

        lobbyArea = ConfiguredArea.load(plugin.getConfig().getConfigurationSection("areas.lobby"));
        arenaArea = ConfiguredArea.load(plugin.getConfig().getConfigurationSection("areas.arena"));

        arenaSpawns.clear();
        arenaSpawns.addAll(ConfigLocationUtil
                .readLocationList(plugin.getConfig().getConfigurationSection("arena-spawns")));
    }

    public GameState getState() {
        return state;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public ConfiguredArea getLobbyArea() {
        return lobbyArea;
    }

    public ConfiguredArea getArenaArea() {
        return arenaArea;
    }

    public List<Location> getArenaSpawns() {
        return new ArrayList<>(arenaSpawns);
    }

    public boolean isInQueue(Player player) {
        return queue.contains(player.getUniqueId());
    }

    public boolean isParticipant(Player player) {
        return activePlayers.contains(player.getUniqueId());
    }

    public boolean isHunter(Player player) {
        return roles.getOrDefault(player.getUniqueId(), PlayerRole.RUNNER) == PlayerRole.HUNTER;
    }

    public void enqueue(Player player) {
        if (!state.canJoin()) {
            player.sendMessage(plugin.getMessage("messages.cannot-join"));
            return;
        }
        if (queue.contains(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("messages.already-queued"));
            return;
        }
        if (queue.size() >= maxPlayers) {
            player.sendMessage(plugin.getMessage("messages.queue-full"));
            return;
        }
        queue.add(player.getUniqueId());
        player.sendMessage(plugin.getMessage("messages.joined-queue", Map.of("position", queue.size())));
        checkAutoStart();
    }

    public void leaveQueue(Player player) {
        if (queue.remove(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("messages.left-queue"));
            if (state == GameState.COUNTDOWN && queue.size() < minPlayers) {
                cancelCountdown();
                broadcastToQueue(plugin.getMessage("messages.countdown-cancelled"));
            }
            return;
        }
        if (isParticipant(player)) {
            eliminatePlayer(player, false);
        } else {
            player.sendMessage(plugin.getMessage("messages.not-in-queue"));
        }
    }

    public void checkAutoStart() {
        if (state != GameState.WAITING) {
            return;
        }
        if (queue.size() < minPlayers) {
            return;
        }
        startCountdown();
    }

    public void startCountdown() {
        if (state == GameState.COUNTDOWN || state == GameState.RUNNING) {
            return;
        }
        if (queue.size() < minPlayers) {
            return;
        }
        state = GameState.COUNTDOWN;
        countdownSecondsRemaining = autoStartSeconds;
        broadcastToQueue(plugin.getMessage("messages.countdown-start",
                Map.of("seconds", countdownSecondsRemaining)));
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (countdownSecondsRemaining <= 0) {
                    cancel();
                    beginMatch();
                    return;
                }
                if (queue.size() < minPlayers) {
                    broadcastToQueue(plugin.getMessage("messages.countdown-cancelled"));
                    cancelCountdown();
                    return;
                }
                if (countdownSecondsRemaining <= 10 || countdownSecondsRemaining % 30 == 0) {
                    broadcastToQueue(plugin.getMessage("messages.countdown-tick",
                            Map.of("seconds", countdownSecondsRemaining)));
                }
                countdownSecondsRemaining--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public boolean forceStart() {
        if (state == GameState.RUNNING) {
            return false;
        }
        if (queue.size() < minPlayers) {
            broadcastToQueue(plugin.getMessage("messages.not-enough-players"));
            return false;
        }
        beginMatch();
        return true;
    }

    private void beginMatch() {
        cancelCountdown();
        if (!validateConfiguration()) {
            broadcastToQueue(plugin.getMessage("messages.configuration-missing"));
            state = GameState.WAITING;
            return;
        }
        state = GameState.RUNNING;
        roles.clear();
        activePlayers.clear();
        for (UUID uuid : queue) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                activePlayers.add(uuid);
            }
        }
        queue.clear();
        if (activePlayers.size() < minPlayers) {
            broadcastToQueue(plugin.getMessage("messages.not-enough-players"));
            state = GameState.WAITING;
            return;
        }
        for (UUID uuid : activePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                roles.put(uuid, PlayerRole.RUNNER);
                applyRunnerState(player);
                teleportToArena(player);
                player.sendMessage(plugin.getMessage("messages.game-start-runner"));
            }
        }
        broadcastToParticipants(plugin.getMessage("messages.game-start"));
        scheduleHunterSelection();
        scheduleGameTimer();
    }

    private void scheduleHunterSelection() {
        cancelHunterSelection();
        hunterSelectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                selectRandomHunter();
            }
        }.runTaskLater(plugin, hunterDelaySeconds * 20L);
    }

    private void scheduleGameTimer() {
        cancelGameTimer();
        gameTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                concludeGame(GameEndReason.RUNNERS_SURVIVED);
            }
        }.runTaskLater(plugin, gameDurationSeconds * 20L);
    }

    public void selectRandomHunter() {
        List<Player> runners = activePlayers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(player -> roles.getOrDefault(player.getUniqueId(), PlayerRole.RUNNER) == PlayerRole.RUNNER)
                .collect(Collectors.toList());
        if (runners.isEmpty()) {
            concludeGame(GameEndReason.CONFIGURATION_ERROR);
            return;
        }
        Player chosen = runners.get(ThreadLocalRandom.current().nextInt(runners.size()));
        promoteToHunter(chosen, true);
        broadcastToParticipants(plugin.getMessage("messages.hunter-selected",
                Map.of("player", chosen.getName())));
    }

    public void handleHunterHit(Player hunter, Player target) {
        if (state != GameState.RUNNING) {
            return;
        }
        if (!isHunter(hunter)) {
            return;
        }
        if (!isParticipant(target)) {
            return;
        }
        if (isHunter(target)) {
            return;
        }
        promoteToHunter(target, false);
        broadcastToParticipants(plugin.getMessage("messages.runner-converted",
                Map.of("player", target.getName())));
        if (getRemainingRunners().isEmpty()) {
            concludeGame(GameEndReason.HUNTERS_ELIMINATED_ALL);
        }
    }

    public void eliminatePlayer(Player player, boolean silent) {
        roles.remove(player.getUniqueId());
        activePlayers.remove(player.getUniqueId());
        resetPlayerState(player);
        teleportToLobby(player);
        if (!silent) {
            broadcastToParticipants(plugin.getMessage("messages.player-left",
                    Map.of("player", player.getName())));
        }
        if (state == GameState.RUNNING && getRemainingRunners().isEmpty()) {
            concludeGame(GameEndReason.HUNTERS_ELIMINATED_ALL);
        }
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        boolean removedFromQueue = queue.remove(uuid);
        if (removedFromQueue && state == GameState.COUNTDOWN && queue.size() < minPlayers) {
            cancelCountdown();
            broadcastToQueue(plugin.getMessage("messages.countdown-cancelled"));
        }
        if (activePlayers.remove(uuid)) {
            roles.remove(uuid);
            if (state == GameState.RUNNING && getRemainingRunners().isEmpty()) {
                concludeGame(GameEndReason.HUNTERS_ELIMINATED_ALL);
            }
        }
    }

    public void concludeGame(GameEndReason reason) {
        if (state != GameState.RUNNING && state != GameState.COUNTDOWN) {
            return;
        }
        cancelCountdown();
        cancelHunterSelection();
        cancelGameTimer();
        state = GameState.ENDING;
        String message = switch (reason) {
            case HUNTERS_ELIMINATED_ALL -> plugin.getMessage("messages.hunters-win");
            case RUNNERS_SURVIVED -> plugin.getMessage("messages.runners-win");
            case MANUAL_STOP -> plugin.getMessage("messages.manual-stop");
            case CONFIGURATION_ERROR -> plugin.getMessage("messages.configuration-missing");
        };
        broadcastToParticipants(message);
        for (UUID uuid : new ArrayList<>(activePlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                resetPlayerState(player);
                teleportToLobby(player);
            }
        }
        activePlayers.clear();
        roles.clear();
        state = GameState.WAITING;
    }

    public boolean stopManually() {
        if (state == GameState.WAITING) {
            return false;
        }
        if (state == GameState.COUNTDOWN) {
            cancelCountdown();
            broadcastToQueue(plugin.getMessage("messages.countdown-cancelled"));
            return true;
        }
        concludeGame(GameEndReason.MANUAL_STOP);
        return true;
    }

    private void teleportToArena(Player player) {
        Location target = pickArenaSpawn().orElseGet(() -> arenaArea.getRandomLocation());
        player.teleport(target);
    }

    private Optional<Location> pickArenaSpawn() {
        if (arenaSpawns.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(arenaSpawns.get(ThreadLocalRandom.current().nextInt(arenaSpawns.size())));
    }

    private void teleportToLobby(Player player) {
        if (lobbyArea != null && lobbyArea.isComplete()) {
            player.teleport(lobbyArea.getCenter());
        } else {
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    private void applyRunnerState(Player player) {
        applyScale(player, runnerScale);
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        player.setFoodLevel(20);
    }

    private void promoteToHunter(Player player, boolean announce) {
        roles.put(player.getUniqueId(), PlayerRole.HUNTER);
        applyScale(player, hunterScale);
        teleportToArena(player);
        if (announce) {
            player.sendMessage(plugin.getMessage("messages.you-are-hunter"));
        } else {
            player.sendMessage(plugin.getMessage("messages.now-hunter"));
        }
    }

    private void resetPlayerState(Player player) {
        applyScale(player, 1.0F);
    }

    private List<Player> getRemainingRunners() {
        return activePlayers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(player -> roles.getOrDefault(player.getUniqueId(), PlayerRole.RUNNER) == PlayerRole.RUNNER)
                .collect(Collectors.toList());
    }

    private boolean validateConfiguration() {
        return lobbyArea != null && lobbyArea.isComplete() && arenaArea != null && arenaArea.isComplete()
                && !arenaSpawns.isEmpty();
    }

    public void cancelAllTasks() {
        cancelCountdown();
        cancelHunterSelection();
        cancelGameTimer();
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (state == GameState.COUNTDOWN) {
            state = GameState.WAITING;
        }
    }

    private void cancelHunterSelection() {
        if (hunterSelectionTask != null) {
            hunterSelectionTask.cancel();
            hunterSelectionTask = null;
        }
    }

    private void cancelGameTimer() {
        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }
    }

    private void broadcastToQueue(String message) {
        broadcast(queue, message);
    }

    private void broadcastToParticipants(String message) {
        broadcast(activePlayers, message);
    }

    private void broadcast(Collection<UUID> recipients, String message) {
        for (UUID uuid : recipients) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && message != null) {
                player.sendMessage(message);
            }
        }
    }

    public void saveLobbyCorner(Location location, boolean first) {
        if (lobbyArea == null) {
            lobbyArea = new ConfiguredArea();
        }
        if (first) {
            lobbyArea.setPos1(location);
        } else {
            lobbyArea.setPos2(location);
        }
        persistAreas();
    }

    public void saveArenaCorner(Location location, boolean first) {
        if (arenaArea == null) {
            arenaArea = new ConfiguredArea();
        }
        if (first) {
            arenaArea.setPos1(location);
        } else {
            arenaArea.setPos2(location);
        }
        persistAreas();
    }

    public void addArenaSpawn(Location location) {
        arenaSpawns.add(location);
        persistArenaSpawns();
    }

    private void persistAreas() {
        ConfigurationSection areas = plugin.getConfig().getConfigurationSection("areas");
        if (areas == null) {
            areas = plugin.getConfig().createSection("areas");
        }
        ConfigurationSection lobbySection = areas.getConfigurationSection("lobby");
        if (lobbySection == null) {
            lobbySection = areas.createSection("lobby");
        }
        ConfigurationSection arenaSection = areas.getConfigurationSection("arena");
        if (arenaSection == null) {
            arenaSection = areas.createSection("arena");
        }
        if (lobbyArea != null) {
            lobbyArea.save(lobbySection);
        }
        if (arenaArea != null) {
            arenaArea.save(arenaSection);
        }
        plugin.saveConfig();
    }

    private void persistArenaSpawns() {
        ConfigurationSection spawns = plugin.getConfig().getConfigurationSection("arena-spawns");
        if (spawns == null) {
            spawns = plugin.getConfig().createSection("arena-spawns");
        }
        ConfigLocationUtil.writeLocationList(spawns, arenaSpawns);
        plugin.saveConfig();
    }

    private void applyScale(Player player, float scale) {
        Attribute attribute = resolveScaleAttribute();
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(scale);
        }
    }

    private Attribute resolveScaleAttribute() {
        if (!scaleAttributeResolved) {
            try {
                cachedScaleAttribute = Attribute.valueOf("GENERIC_SCALE");
            } catch (IllegalArgumentException ex) {
                cachedScaleAttribute = null;
                if (!scaleWarningLogged) {
                    plugin.getLogger().warning("GENERIC_SCALE attribute not available; player scaling will be skipped.");
                    scaleWarningLogged = true;
                }
            }
            scaleAttributeResolved = true;
        }
        return cachedScaleAttribute;
    }
}
