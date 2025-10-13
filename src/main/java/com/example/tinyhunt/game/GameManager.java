package com.example.tinyhunt.game;

import com.example.tinyhunt.TinyHuntPlugin;
import com.example.tinyhunt.model.ArenaDefinition;
import com.example.tinyhunt.model.ConfiguredArea;
import com.example.tinyhunt.model.PlayerRole;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Coordinates queue management, game flow, and configuration-backed state.
 */
public final class GameManager {

    private final TinyHuntPlugin plugin;
    private ConfiguredArea lobbyArea;
    private final Map<String, ArenaDefinition> arenas = new LinkedHashMap<>();
    private String activeArenaName;

    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private final LinkedHashSet<UUID> activePlayers = new LinkedHashSet<>();
    private final Map<UUID, PlayerRole> roles = new HashMap<>();
    private final Map<UUID, BukkitTask> pendingConversions = new HashMap<>();
    private final Map<UUID, GameMode> storedModes = new HashMap<>();

    private final MatchHud matchHud;
    private final JoinMenu joinMenu;
    private final ArenaSetupManager arenaSetupManager;

    private GameState state = GameState.WAITING;
    private BukkitTask countdownTask;
    private int countdownSecondsRemaining;
    private BukkitTask hunterSelectionTask;
    private BukkitTask gameTimerTask;
    private BukkitTask hudUpdateTask;
    private long matchStartMillis;
    private long matchEndMillis;
    private boolean suddenDeathTriggered;
    private long nextRevealMillis;

    private int minPlayers;
    private int maxPlayers;
    private int autoStartSeconds;
    private int hunterDelaySeconds;
    private int gameDurationSeconds;
    private int conversionDelaySeconds;
    private int conversionInvulnerabilitySeconds;
    private boolean suddenDeathEnabled;
    private int suddenDeathStartSeconds;
    private int suddenDeathRevealIntervalSeconds;
    private int suddenDeathRevealDurationSeconds;
    private int suddenDeathHunterSpeedAmplifier;
    private float runnerScale;
    private float hunterScale;
    private Attribute cachedScaleAttribute;
    private boolean scaleAttributeResolved;
    private boolean scaleWarningLogged;

    public GameManager(TinyHuntPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.matchHud = new MatchHud(plugin);
        this.joinMenu = new JoinMenu(plugin, this);
        this.arenaSetupManager = new ArenaSetupManager(plugin, this);
        reloadSettings();
    }

    public void reloadSettings() {
        plugin.reloadConfig();
        minPlayers = Math.max(2, plugin.getConfig().getInt("players.min", 4));
        maxPlayers = Math.max(minPlayers, plugin.getConfig().getInt("players.max", minPlayers));
        autoStartSeconds = Math.max(5, plugin.getConfig().getInt("timers.auto-start-seconds", 120));
        hunterDelaySeconds = Math.max(1, plugin.getConfig().getInt("timers.hunter-selection-seconds", 10));
        gameDurationSeconds = Math.max(30, plugin.getConfig().getInt("timers.game-duration-seconds", 600));
        conversionDelaySeconds = Math.max(1, plugin.getConfig().getInt("timers.runner-respawn-seconds", 5));
        conversionInvulnerabilitySeconds = Math.max(0,
                plugin.getConfig().getInt("timers.respawn-invulnerability-seconds", 2));
        suddenDeathEnabled = plugin.getConfig().getBoolean("sudden-death.enabled", true);
        suddenDeathStartSeconds = Math.max(10, plugin.getConfig().getInt("sudden-death.start-seconds", 120));
        suddenDeathRevealIntervalSeconds = Math.max(5,
                plugin.getConfig().getInt("sudden-death.reveal-interval-seconds", 20));
        suddenDeathRevealDurationSeconds = Math.max(1,
                plugin.getConfig().getInt("sudden-death.reveal-duration-seconds", 5));
        suddenDeathHunterSpeedAmplifier = Math.max(0,
                plugin.getConfig().getInt("sudden-death.hunter-speed-amplifier", 1));
        runnerScale = (float) plugin.getConfig().getDouble("scales.runner", 0.33D);
        hunterScale = (float) plugin.getConfig().getDouble("scales.hunter", 1.0D);

        lobbyArea = ConfiguredArea.load(plugin.getConfig().getConfigurationSection("areas.lobby"));

        arenas.clear();
        ConfigurationSection arenasSection = plugin.getConfig().getConfigurationSection("arenas");
        if (arenasSection != null) {
            for (String key : arenasSection.getKeys(false)) {
                ArenaDefinition definition = ArenaDefinition.load(key, arenasSection.getConfigurationSection(key));
                arenas.put(key, definition);
            }
        }
        activeArenaName = plugin.getConfig().getString("active-arena");
        if (activeArenaName == null || !arenas.containsKey(activeArenaName)) {
            activeArenaName = arenas.keySet().stream().findFirst().orElse(null);
        }
    }

    public GameState getState() {
        return state;
    }

    public TinyHuntPlugin getPlugin() {
        return plugin;
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

    public JoinMenu getJoinMenu() {
        return joinMenu;
    }

    public ArenaSetupManager getArenaSetupManager() {
        return arenaSetupManager;
    }

    public Optional<ArenaDefinition> getActiveArena() {
        if (activeArenaName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(arenas.get(activeArenaName));
    }

    public List<String> getArenaNames() {
        return new ArrayList<>(arenas.keySet());
    }

    public boolean isInQueue(Player player) {
        return queue.contains(player.getUniqueId());
    }

    public int getQueueSize() {
        return queue.size();
    }

    public boolean isParticipant(Player player) {
        return activePlayers.contains(player.getUniqueId());
    }

    public int getParticipantCount() {
        return activePlayers.size();
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
        suddenDeathTriggered = false;
        nextRevealMillis = 0L;
        matchStartMillis = System.currentTimeMillis();
        matchEndMillis = matchStartMillis + gameDurationSeconds * 1000L;
        scheduleHunterSelection();
        scheduleGameTimer();
        matchHud.start(activePlayers);
        startHudUpdates();
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
        if (isHunter(target) || isConverting(target)) {
            return;
        }
        beginRunnerConversion(target);
    }

    public void eliminatePlayer(Player player, boolean silent) {
        cancelConversion(player.getUniqueId());
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
            cancelConversion(uuid);
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
        cancelHudUpdates();
        cancelConversions();
        matchHud.stop();
        suddenDeathTriggered = false;
        nextRevealMillis = 0L;
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
        ArenaDefinition arena = getActiveArena()
                .orElseThrow(() -> new IllegalStateException("Active arena not configured"));
        Location target = pickArenaSpawn(arena).orElseGet(() -> arena.getArea().getRandomLocation());
        player.teleport(target);
    }

    private Optional<Location> pickArenaSpawn(ArenaDefinition arena) {
        List<Location> spawns = arena.getSpawns();
        if (spawns.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(spawns.get(ThreadLocalRandom.current().nextInt(spawns.size())));
    }

    private void teleportToLobby(Player player) {
        if (lobbyArea != null && lobbyArea.isComplete()) {
            player.teleport(lobbyArea.getCenter());
        } else {
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    private void applyRunnerState(Player player) {
        restoreGameMode(player);
        applyScale(player, runnerScale);
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        player.setFoodLevel(20);
    }

    private void promoteToHunter(Player player, boolean announce) {
        roles.put(player.getUniqueId(), PlayerRole.HUNTER);
        restoreGameMode(player);
        applyScale(player, hunterScale);
        teleportToArena(player);
        applyHunterBuffs(player);
        if (announce) {
            player.sendMessage(plugin.getMessage("messages.you-are-hunter"));
        } else {
            player.sendMessage(plugin.getMessage("messages.now-hunter"));
        }
    }

    private void resetPlayerState(Player player) {
        applyScale(player, 1.0F);
        restoreGameMode(player);
        player.setNoDamageTicks(0);
        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }
    }

    private List<Player> getRemainingRunners() {
        return activePlayers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(player -> roles.getOrDefault(player.getUniqueId(), PlayerRole.RUNNER) == PlayerRole.RUNNER)
                .collect(Collectors.toList());
    }

    private int getRunnerCount() {
        return (int) activePlayers.stream()
                .map(uuid -> roles.getOrDefault(uuid, PlayerRole.RUNNER))
                .filter(role -> role == PlayerRole.RUNNER)
                .count();
    }

    private int getHunterCount() {
        return (int) activePlayers.stream()
                .map(uuid -> roles.getOrDefault(uuid, PlayerRole.RUNNER))
                .filter(role -> role == PlayerRole.HUNTER)
                .count();
    }

    private boolean validateConfiguration() {
        if (lobbyArea == null || !lobbyArea.isComplete()) {
            return false;
        }
        return getActiveArena()
                .filter(arena -> arena.getArea().isComplete() && !arena.getSpawns().isEmpty())
                .isPresent();
    }

    public void cancelAllTasks() {
        cancelCountdown();
        cancelHunterSelection();
        cancelGameTimer();
        cancelHudUpdates();
        cancelConversions();
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

    private boolean isConverting(Player player) {
        return roles.getOrDefault(player.getUniqueId(), PlayerRole.RUNNER) == PlayerRole.CONVERTING;
    }

    private void beginRunnerConversion(Player player) {
        UUID uuid = player.getUniqueId();
        roles.put(uuid, PlayerRole.CONVERTING);
        storedModes.put(uuid, player.getGameMode());
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(plugin.getMessage("messages.runner-respawn-start",
                Map.of("seconds", conversionDelaySeconds)));
        broadcastToParticipants(plugin.getMessage("messages.runner-respawn-broadcast",
                Map.of("player", player.getName(), "seconds", conversionDelaySeconds)));
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                finishRunnerConversion(uuid);
            }
        }.runTaskLater(plugin, conversionDelaySeconds * 20L);
        pendingConversions.put(uuid, task);
    }

    private void finishRunnerConversion(UUID uuid) {
        pendingConversions.remove(uuid);
        if (state != GameState.RUNNING) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        roles.put(uuid, PlayerRole.HUNTER);
        restoreGameMode(player);
        applyScale(player, hunterScale);
        teleportToArena(player);
        if (conversionInvulnerabilitySeconds > 0) {
            player.setNoDamageTicks(conversionInvulnerabilitySeconds * 20);
        }
        applyHunterBuffs(player);
        player.sendMessage(plugin.getMessage("messages.runner-respawn-complete"));
        broadcastToParticipants(plugin.getMessage("messages.runner-converted", Map.of("player", player.getName())));
        if (getRemainingRunners().isEmpty()) {
            concludeGame(GameEndReason.HUNTERS_ELIMINATED_ALL);
        }
    }

    private void cancelConversion(UUID uuid) {
        BukkitTask task = pendingConversions.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        storedModes.remove(uuid);
    }

    private void cancelConversions() {
        for (BukkitTask task : pendingConversions.values()) {
            task.cancel();
        }
        pendingConversions.clear();
        storedModes.clear();
    }

    private void startHudUpdates() {
        cancelHudUpdates();
        hudUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateHud();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void cancelHudUpdates() {
        if (hudUpdateTask != null) {
            hudUpdateTask.cancel();
            hudUpdateTask = null;
        }
    }

    private void updateHud() {
        if (state != GameState.RUNNING) {
            return;
        }
        long now = System.currentTimeMillis();
        long timeRemainingSeconds = Math.max(0L, (matchEndMillis - now + 999L) / 1000L);
        if (suddenDeathEnabled && !suddenDeathTriggered && timeRemainingSeconds <= suddenDeathStartSeconds) {
            triggerSuddenDeath();
        }
        long revealSeconds = -1L;
        if (suddenDeathTriggered) {
            if (now >= nextRevealMillis) {
                performSuddenDeathReveal();
            }
            revealSeconds = Math.max(0L, (nextRevealMillis - System.currentTimeMillis() + 999L) / 1000L);
        }
        matchHud.update(activePlayers, createHudSnapshot(timeRemainingSeconds, revealSeconds));
    }

    private MatchHud.HudSnapshot createHudSnapshot(long timeRemainingSeconds, long nextRevealSeconds) {
        String formattedTime = formatDuration(timeRemainingSeconds);
        String bossBarTemplate = getHudString("hud.bossbar-title", "&6Tempo: &e%time%");
        String bossBarTitle = ChatColor.stripColor(bossBarTemplate.replace("%time%", formattedTime));
        String scoreboardTitle = ChatColor.stripColor(getHudString("hud.scoreboard-title", "TinyHunt"));
        String extraLine = suddenDeathTriggered
                ? ChatColor.stripColor(getHudString("hud.extra-sudden-death", "Sudden death!"))
                : ChatColor.stripColor(getHudString("hud.extra-match", ""));
        double progress = gameDurationSeconds <= 0 ? 0.0D
                : Math.max(0.0D, Math.min(1.0D, (double) timeRemainingSeconds / gameDurationSeconds));
        return new MatchHud.HudSnapshot(bossBarTitle, scoreboardTitle, progress, formattedTime, getRunnerCount(),
                getHunterCount(), nextRevealSeconds, extraLine);
    }

    private void triggerSuddenDeath() {
        suddenDeathTriggered = true;
        broadcastToParticipants(plugin.getMessage("messages.sudden-death-start"));
        nextRevealMillis = System.currentTimeMillis();
        for (UUID uuid : activePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && roles.getOrDefault(uuid, PlayerRole.RUNNER) == PlayerRole.HUNTER) {
                applyHunterBuffs(player);
            }
        }
        performSuddenDeathReveal();
    }

    private void performSuddenDeathReveal() {
        nextRevealMillis = System.currentTimeMillis() + suddenDeathRevealIntervalSeconds * 1000L;
        for (Player runner : getRemainingRunners()) {
            runner.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,
                    suddenDeathRevealDurationSeconds * 20, 0, true, false, true));
        }
        if (!getRemainingRunners().isEmpty()) {
            broadcastToParticipants(plugin.getMessage("messages.sudden-death-reveal",
                    Map.of("seconds", suddenDeathRevealDurationSeconds)));
        }
    }

    private void applyHunterBuffs(Player player) {
        if (!suddenDeathTriggered || suddenDeathHunterSpeedAmplifier <= 0) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, suddenDeathRevealIntervalSeconds * 20,
                suddenDeathHunterSpeedAmplifier - 1, true, false, true));
    }

    private void restoreGameMode(Player player) {
        GameMode previous = storedModes.remove(player.getUniqueId());
        if (previous != null) {
            player.setGameMode(previous);
        } else if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private String getHudString(String path, String def) {
        String value = plugin.getConfig().getString(path, def);
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private String formatDuration(long seconds) {
        long clamped = Math.max(0, seconds);
        long minutes = clamped / 60;
        long remaining = clamped % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, remaining);
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
        persistLobby();
    }

    public boolean saveArenaCorner(String arenaName, Location location, boolean first) {
        ArenaDefinition arena = arenas.get(arenaName);
        if (arena == null) {
            return false;
        }
        if (first) {
            arena.getArea().setPos1(location);
        } else {
            arena.getArea().setPos2(location);
        }
        persistArenas();
        return true;
    }

    public boolean addArenaSpawn(String arenaName, Location location) {
        ArenaDefinition arena = arenas.get(arenaName);
        if (arena == null) {
            return false;
        }
        arena.addSpawn(location);
        persistArenas();
        return true;
    }

    public boolean arenaExists(String arenaName) {
        return arenas.containsKey(arenaName);
    }

    public boolean createArena(String arenaName) {
        if (arenas.containsKey(arenaName)) {
            return false;
        }
        arenas.put(arenaName, new ArenaDefinition(arenaName));
        if (activeArenaName == null) {
            activeArenaName = arenaName;
        }
        persistArenas();
        return true;
    }

    public boolean setActiveArena(String arenaName) {
        if (!arenas.containsKey(arenaName)) {
            return false;
        }
        activeArenaName = arenaName;
        persistArenas();
        return true;
    }

    private void persistLobby() {
        ConfigurationSection areas = plugin.getConfig().getConfigurationSection("areas");
        if (areas == null) {
            areas = plugin.getConfig().createSection("areas");
        }
        ConfigurationSection lobbySection = areas.getConfigurationSection("lobby");
        if (lobbySection == null) {
            lobbySection = areas.createSection("lobby");
        }
        if (lobbyArea != null) {
            lobbyArea.save(lobbySection);
        }
        plugin.saveConfig();
    }

    private void persistArenas() {
        ConfigurationSection arenasSection = plugin.getConfig().getConfigurationSection("arenas");
        if (arenasSection == null) {
            arenasSection = plugin.getConfig().createSection("arenas");
        }
        for (String key : new ArrayList<>(arenasSection.getKeys(false))) {
            arenasSection.set(key, null);
        }
        for (ArenaDefinition arena : arenas.values()) {
            ConfigurationSection section = arenasSection.createSection(arena.getName());
            arena.save(section);
        }
        plugin.getConfig().set("active-arena", activeArenaName);
        ConfigurationSection areas = plugin.getConfig().getConfigurationSection("areas");
        if (areas != null) {
            areas.set("arena", null);
        }
        plugin.getConfig().set("arena-spawns", null);
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
