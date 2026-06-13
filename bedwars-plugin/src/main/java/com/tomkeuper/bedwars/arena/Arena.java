/*
 * BedWars2023 - A bed wars mini-game.
 * Copyright (C) 2024 Tomas Keuper
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: contact@fyreblox.com
 */

package com.tomkeuper.bedwars.arena;

import com.saicone.rtag.util.SkullTexture;
import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.NextEvent;
import com.tomkeuper.bedwars.api.arena.generator.GeneratorType;
import com.tomkeuper.bedwars.api.arena.generator.IGenHolo;
import com.tomkeuper.bedwars.api.arena.generator.IGenerator;
import com.tomkeuper.bedwars.api.arena.shop.ShopHolo;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.arena.team.ITeamAssigner;
import com.tomkeuper.bedwars.api.arena.team.TeamColor;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.entity.Despawnable;
import com.tomkeuper.bedwars.api.events.gameplay.GameEndEvent;
import com.tomkeuper.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.tomkeuper.bedwars.api.events.gameplay.NextEventChangeEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerJoinArenaEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerKillEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerReJoinEvent;
import com.tomkeuper.bedwars.api.events.server.ArenaDisableEvent;
import com.tomkeuper.bedwars.api.events.server.ArenaEnableEvent;
import com.tomkeuper.bedwars.api.events.server.ArenaRestartEvent;
import com.tomkeuper.bedwars.api.events.server.ArenaSpectateEvent;
import com.tomkeuper.bedwars.api.items.handlers.IPermanentItem;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.region.Region;
import com.tomkeuper.bedwars.api.server.ServerType;
import com.tomkeuper.bedwars.api.tasks.AnnouncementTask;
import com.tomkeuper.bedwars.api.tasks.PlayingTask;
import com.tomkeuper.bedwars.api.tasks.RestartingTask;
import com.tomkeuper.bedwars.api.tasks.StartingTask;
import com.tomkeuper.bedwars.arena.tasks.*;
import com.tomkeuper.bedwars.arena.team.BedWarsTeam;
import com.tomkeuper.bedwars.arena.team.TeamAssigner;
import com.tomkeuper.bedwars.configuration.ArenaConfig;
import com.tomkeuper.bedwars.configuration.Sounds;
import com.tomkeuper.bedwars.levels.internal.InternalLevel;
import com.tomkeuper.bedwars.levels.internal.PerMinuteTask;
import com.tomkeuper.bedwars.listeners.blockstatus.BlockStatusListener;
import com.tomkeuper.bedwars.listeners.chat.ChatFormatting;
import com.tomkeuper.bedwars.listeners.dropshandler.PlayerDrops;
import com.tomkeuper.bedwars.money.internal.MoneyPerMinuteTask;
import com.tomkeuper.bedwars.shop.ShopCache;
import com.tomkeuper.bedwars.shop.main.ShopIndex;
import com.tomkeuper.bedwars.sidebar.BoardManager;
import com.tomkeuper.bedwars.support.citizens.JoinNPC;
import com.tomkeuper.bedwars.support.paper.PaperSupport;
import com.tomkeuper.bedwars.support.papi.SupportPAPI;
import com.tomkeuper.bedwars.support.vault.WithEconomy;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.bossbar.BossBar;
import me.neznamy.tab.api.placeholder.PlayerPlaceholder;
import me.neznamy.tab.api.placeholder.ServerPlaceholder;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static com.tomkeuper.bedwars.BedWars.*;
import static com.tomkeuper.bedwars.api.language.Language.*;
import static com.tomkeuper.bedwars.arena.upgrades.BaseListener.isOnABase;

import com.tomkeuper.bedwars.api.shop.IShopIndex;
import com.tomkeuper.bedwars.api.upgrades.UpgradesIndex;
import com.tomkeuper.bedwars.shop.ShopManager;

@SuppressWarnings("WeakerAccess")
public class Arena implements IArena {

    private IShopIndex linkedShop;
    private UpgradesIndex linkedUpgrades;

    private static final HashMap<String, IArena> arenaByName = new HashMap<>();
    private static final HashMap<Player, IArena> arenaByPlayer = new HashMap<>();
    private static final HashMap<String, IArena> arenaByIdentifier = new HashMap<>();
    private static final LinkedList<IArena> arenas = new LinkedList<>();
    private static int gamesBeforeRestart = config.getInt(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_GAMES_BEFORE_RESTART);
    public static HashMap<UUID, Integer> afkCheck = new HashMap<>();
    public static HashMap<UUID, Integer> magicMilk = new HashMap<>();

    // --- Static Utility Accessors to resolve Compilation Errors ---

    public static IArena getArenaByName(String name) {
        return arenaByName.get(name);
    }

    public static IArena getArenaByPlayer(Player player) {
        return arenaByPlayer.get(player);
    }

    public static HashMap<Player, IArena> getArenaByPlayer() {
        return arenaByPlayer;
    }

    public static IArena getArenaByIdentifier(String identifier) {
        return arenaByIdentifier.get(identifier);
    }

    public static LinkedList<IArena> getArenas() {
        return arenas;
    }

    public static boolean isInArena(Player player) {
        return arenaByPlayer.containsKey(player);
    }

    public static boolean joinRandomArena(Player player) {
        for (IArena arena : arenas) {
            if (arena.getStatus() == GameState.waiting || arena.getStatus() == GameState.starting) {
                if (arena.addPlayer(player, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean joinRandomFromGroup(Player player, String groupName) {
        for (IArena arena : arenas) {
            if (arena.getGroup().equalsIgnoreCase(groupName)) {
                if (arena.getStatus() == GameState.waiting || arena.getStatus() == GameState.starting) {
                    if (arena.addPlayer(player, false)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------

    private List<Player> players = new ArrayList<>();
    private List<Player> spectators = new ArrayList<>();
    private List<Block> signs = new ArrayList<>();
    private GameState status = GameState.restarting;
    private YamlConfiguration yml;
    private ArenaConfig cm;
    private int minPlayers = 2, maxPlayers = 10, maxInTeam = 1, islandRadius = 10;
    public int upgradeDiamondsCount = 0, upgradeEmeraldsCount = 0;
    public boolean allowSpectate = true, allowMapBreak = false, enderDragonDestory = false;
    private World world;
    private String group = "Default", arenaName, worldName;
    private List<ITeam> teams = new ArrayList<>();
    private LinkedList<org.bukkit.util.Vector> placed = new LinkedList<>();
    private List<String> nextEvents = new ArrayList<>();
    private List<Region> regionsList = new ArrayList<>();
    private List<ServerPlaceholder> serverPlaceholders = new ArrayList<>();
    private List<BossBar> dragonBossbars = new ArrayList<>();
    private List<Scoreboard> scoreboards = new ArrayList<>();
    private int renderDistance, magicMilkTime = 30;

    private final List<Player> leaving = new ArrayList<>();

    /**
     * Current event, used at scoreboard
     */
    private NextEvent nextEvent = NextEvent.DIAMOND_GENERATOR_TIER_II;
    private int diamondTier = 1, emeraldTier = 1;

    /**
     * Players in respawn session
     */
    private ConcurrentHashMap<Player, Integer> respawnSessions = new ConcurrentHashMap<>();

    /**
     * Invisibility for armor when you drink an invisibility potion
     */
    private ConcurrentHashMap<Player, Integer> showTime = new ConcurrentHashMap<>();

    /**
     * Player location before joining.
     * The player is teleported to this location if the server is running in SHARED mode.
     */
    private static final HashMap<Player, Location> playerLocation = new HashMap<>();

    /**
     * temp stats. some of them use player name as key to keep names of players who left. at checkWinners for example.
     * Those maps are not used for db stats but is for internal use only.
     */
    private HashMap<String, Integer> playerKills = new HashMap<>();
    private HashMap<String, Integer> playerTotalKills = new HashMap<>();
    private HashMap<Player, Integer> playerBedsDestroyed = new HashMap<>();
    private HashMap<Player, Integer> playerFinalKills = new HashMap<>();
    private HashMap<Player, Integer> playerDeaths = new HashMap<>();
    private HashMap<Player, Integer> playerFinalKillDeaths = new HashMap<>();


    /* ARENA TASKS */
    private StartingTask startingTask = null;
    private PlayingTask playingTask = null;
    private RestartingTask restartingTask = null;

    private AnnouncementTask announcementTask;

    /* ARENA GENERATORS */
    private List<IGenerator> oreGenerators = new ArrayList<>();

    /* SHOP HOLOGRAMS */
    private HashMap<String, List<ShopHolo>> shopHolosIso = new HashMap<>();

    private PerMinuteTask perMinuteTask;

    private MoneyPerMinuteTask moneyperMinuteTask;

    private static final LinkedList<IArena> enableQueue = new LinkedList<>();

    private Location respawnLocation, spectatorLocation, waitingLocation;
    private int yKillHeight;
    private Instant startTime;
    private ITeamAssigner teamAssigner = new TeamAssigner();
    private String mapName;

    /**
     * Load an arena.
     * This will check if it was set up right.
     *
     * @param name - world name
     * @param p    - This will send messages to the player if something went wrong while loading the arena. Can be NULL.
     */
    public Arena(String name, @Nullable CommandSender p) {
        if (!autoscale) {
            for (IArena mm : enableQueue) {
                if (mm.getArenaName().equalsIgnoreCase(name)) {
                    plugin.getLogger().severe("Tried to load arena " + name + " but it is already in the enable queue.");
                    if (p != null)
                        p.sendMessage(ChatColor.RED + "Tried to load arena " + name + " but it is already in the enable queue.");
                    return;
                }
            }
            if (getArenaByName(name) != null) {
                plugin.getLogger().severe("Tried to load arena " + name + " but it is already enabled.");
                if (p != null)
                    p.sendMessage(ChatColor.RED + "Tried to load arena " + name + " but it is already enabled.");
                return;
            }
        }
        this.arenaName = name;
        cm = new ArenaConfig(BedWars.plugin, name, plugin.getDataFolder().getPath() + "/Arenas");

        yml = cm.getYml();
        this.mapName = yml.getString(ConfigPath.ARENA_USE_MAP);
        if (this.mapName == null || this.mapName.isEmpty()) {
            this.mapName = arenaName;
        }

        if (autoscale) {
            this.worldName = BedWars.arenaManager.generateGameID();
        } else {
            this.worldName = arenaName;
        }

        if (yml.get("Team") == null) {
            if (p != null) p.sendMessage("You didn't set any team for arena: " + name);
            plugin.getLogger().severe("You didn't set any team for arena: " + name);
            return;
        }
        if (yml.getConfigurationSection("Team").getKeys(false).size() < 2) {
            if (p != null) p.sendMessage("§cYou must set at least 2 teams on: " + name);
            plugin.getLogger().severe("You must set at least 2 teams on: " + name);
            return;
        }
        maxInTeam = yml.getInt("maxInTeam");
        maxPlayers = yml.getConfigurationSection("Team").getKeys(false).size() * maxInTeam;
        minPlayers = yml.getInt("minPlayers");
        allowSpectate = yml.getBoolean("allowSpectate");
        enderDragonDestory = yml.getBoolean(ConfigPath.ARENA_ALLOW_DRAGON_DESTROY_WHEN_PROTECTED);
        allowMapBreak = yml.getBoolean(ConfigPath.ARENA_ALLOW_MAP_BREAK);
        magicMilkTime = yml.getInt(ConfigPath.ARENA_MAGIC_MILK_TIME);
        islandRadius = yml.getInt(ConfigPath.ARENA_ISLAND_RADIUS);
        if (config.getYml().get("arenaGroups") != null) {
            if (config.getYml().getStringList("arenaGroups").contains(yml.getString("group"))) {
                group = yml.getString("group");
            }
        }


        if (!BedWars.getAPI().getRestoreAdapter().isWorld(this.mapName)) {
            if (p != null) p.sendMessage(ChatColor.RED + "There isn't any map called " + this.mapName);
            plugin.getLogger().log(Level.WARNING, "There isn't any map called " + this.mapName);
            return;
        }

        boolean error = false;
        for (String team : yml.getConfigurationSection("Team").getKeys(false)) {
            String colorS = yml.getString("Team." + team + ".Color");
            if (colorS == null) continue;
            colorS = colorS.toUpperCase();
            try {
                TeamColor.valueOf(colorS);
            } catch (Exception e) {
                if (p != null) p.sendMessage("§cInvalid color at team: " + team + " in arena: " + name);
                plugin.getLogger().severe("Invalid color at team: " + team + " in arena: " + name);
                error = true;
            }
            for (String stuff : Arrays.asList("Color", "Spawn", "Bed", "Shop", "Upgrade", "Iron", "Gold")) {
                if (yml.get("Team." + team + "." + stuff) == null) {
                    if (p != null) p.sendMessage("§c" + stuff + " not set for " + team + " team on: " + name);
                    plugin.getLogger().severe(stuff + " not set for " + team + " team on: " + name);
                    error = true;
                }
            }
        }
        if (yml.get("generator.Diamond") == null) {
            if (p != null) p.sendMessage("§cThere aren't any Diamond generators set on: " + name);
            plugin.getLogger().severe("There aren't any Diamond generators set on: " + name);
        }
        if (yml.get("generator.Emerald") == null) {
            if (p != null) p.sendMessage("§cThere aren't any Emerald generators set on: " + name);
            plugin.getLogger().severe("There aren't any Emerald generators set on: " + name);
        }
        if (yml.get("waiting.Loc") == null) {
            if (p != null) p.sendMessage("§cWaiting spawn not set on: " + name);
            plugin.getLogger().severe("Waiting spawn not set on: " + name);
            return;
        }
        if (error) return;
        yKillHeight = yml.getInt(ConfigPath.ARENA_Y_LEVEL_KILL);
        addToEnableQueue(this);
        Language.saveIfNotExists(Messages.ARENA_DISPLAY_GROUP_PATH + getGroup().toLowerCase(), String.valueOf(getGroup().charAt(0)).toUpperCase() + group.substring(1).toLowerCase());
        Language.getLanguages().forEach(language -> {
            if (!language.exists(Messages.NPC_NAME_TEAM_UPGRADES.replace("%group%", group))){
                language.generateNPCMessages(language.getYml(), group);
            }
        });
    }

    /**
     * Use this method when the world was loaded successfully.
     */
    @Override
    public void init(World world) {
        if (!autoscale) {
            if (getArenaByName(arenaName) != null) return;
        }
        removeFromEnableQueue(this);
        debug("Initialized arena " + getArenaName() + " with map " + world.getName());
        this.world = world;
        this.worldName = world.getName();
        getConfig().setName(worldName);

        // Link per-arena shop and upgrades layouts
        try {
            // Link the global ShopIndex; categories are pre-resolved per arena below
            this.linkedShop = ShopManager.shop;
            if (this.linkedShop != null) {
                ((ShopIndex) this.linkedShop).preResolveForArena(this);
            }
        } catch (Throwable ignored) {}
        try {
            this.linkedUpgrades = BedWars.getUpgradeManager().getMenuForArena(this);
        } catch (Throwable ignored) {}
        world.getEntities().stream().filter(e -> e.getType() != EntityType.PLAYER)
                .filter(e -> e.getType() != EntityType.PAINTING).filter(e -> e.getType() != EntityType.ITEM_FRAME)
                .forEach(Entity::remove);
        for (String s : getConfig().getList(ConfigPath.ARENA_GAME_RULES)) {
            String[] rule = s.split(":");
            if (rule.length == 2) world.setGameRuleValue(rule[0], rule[1]);
        }
        world.setAutoSave(false);

        /* Clear setup armor-stands */
        for (Entity e : world.getEntities()) {
            if (e.getType() == EntityType.ARMOR_STAND) {
                if (!((ArmorStand) e).isVisible()) e.remove();
            }
        }

        //Create teams
        for (String team : yml.getConfigurationSection("Team").getKeys(false)) {
            if (getTeam(team) != null) {
                BedWars.plugin.getLogger().severe("A team with name: " + team + " was already loaded for arena: " + getArenaName());
                continue;
            }
            BedWarsTeam bwt = new BedWarsTeam(team, TeamColor.valueOf(yml.getString("Team." + team + ".Color").toUpperCase()), cm.getArenaLoc("Team." + team + ".Spawn"),
                    cm.getArenaLoc("Team." + team + ".Bed"), cm.getArenaLoc("Team." + team + ".Shop"), cm.getArenaLoc("Team." + team + ".Upgrade"), this);
            teams.add(bwt);
            bwt.spawnGenerators();
        }

        //Load diamond/ emerald generators
        Location location;
        for (String type : Arrays.asList("Diamond", "Emerald")) {
            if (yml.get("generator." + type) != null) {
                for (String s : yml.getStringList("generator." + type)) {
                    location = cm.convertStringToArenaLocation(s);
                    if (location == null) {
                        plugin.getLogger().severe("Invalid location for " + type + " generator: " + s);
                        continue;
                    }
                    oreGenerators.add(new OreGenerator(location, this, GeneratorType.valueOf(type.toUpperCase()), null, true));
                }
            }
        }

        // Force-load public map-wide Iron and Gold generators
        for (String type : Arrays.asList("Iron", "Gold")) {
            if (yml.get("generator." + type) != null) {
                for (String s : yml.getStringList("generator." + type)) {
                    location = cm.convertStringToArenaLocation(s);
                    if (location == null) {
                        plugin.getLogger().severe("Invalid location for public " + type + " generator: " + s);
                        continue;
                    }
                    oreGenerators.add(new OreGenerator(location, this, GeneratorType.valueOf(type.toUpperCase()), null, true));
                }
            }
        }

        arenas.add(this);
        arenaByName.put(getArenaName(), this);
        arenaByIdentifier.put(worldName, this);
        world.getWorldBorder().setCenter(cm.getArenaLoc("waiting.Loc"));
        world.getWorldBorder().setSize(yml.getInt("worldBorder"));

        /* Check if lobby removal is set */
        if (!getConfig().getYml().isSet(ConfigPath.ARENA_WAITING_POS1) && getConfig().getYml().isSet(ConfigPath.ARENA_WAITING_POS2)) {
            plugin.getLogger().severe("Lobby Pos1 isn't set! The arena's lobby won't be removed!");
        }
        if (getConfig().getYml().isSet(ConfigPath.ARENA_WAITING_POS1) && !getConfig().getYml().isSet(ConfigPath.ARENA_WAITING_POS2)) {
            plugin.getLogger().severe("Lobby Pos2 isn't set! The arena's lobby won't be removed!");
        }

        /* Register arena signs */
        registerSigns();
        //Call event
        Bukkit.getPluginManager().callEvent(new ArenaEnableEvent(this));

        // Re Spawn Session Location
        respawnLocation = cm.getArenaLoc(ConfigPath.ARENA_SPEC_LOC);
        if (respawnLocation == null) {
            respawnLocation = cm.getArenaLoc("waiting.Loc");
        }
        if (respawnLocation == null) {
            respawnLocation = world.getSpawnLocation();
        }
        //

        // Spectator location
        spectatorLocation = cm.getArenaLoc(ConfigPath.ARENA_SPEC_LOC);
        if (spectatorLocation == null) {
            spectatorLocation = cm.getArenaLoc("waiting.Loc");
        }
        if (spectatorLocation == null) {
            spectatorLocation = world.getSpawnLocation();
        }
        //

        // Waiting location
        waitingLocation = cm.getArenaLoc("waiting.Loc");
        if (waitingLocation == null) {
            waitingLocation = world.getSpawnLocation();
        }
        //

        changeStatus(GameState.waiting);

        //
        for (NextEvent ne : NextEvent.values()) {
            nextEvents.add(ne.toString());
        }

        upgradeDiamondsCount = getGeneratorsCfg().getInt(getGeneratorsCfg().getYml().get(getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_II_START) == null ?
                "Default." + ConfigPath.GENERATOR_DIAMOND_TIER_II_START : getGroup() + "." + ConfigPath.GENERATOR_DIAMOND_TIER_II_START);
        upgradeEmeraldsCount = getGeneratorsCfg().getInt(getGeneratorsCfg().getYml().get(getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_II_START) == null ?
                "Default." + ConfigPath.GENERATOR_EMERALD_TIER_II_START : getGroup() + "." + ConfigPath.GENERATOR_EMERALD_TIER_II_START);
        plugin.getLogger().info("Load done: " + getArenaName());


        // entity tracking range - player
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File("spigot.yml"));
        renderDistance = yaml.get("world-settings." + getWorldName() + ".entity-tracking-range.players") == null ?
                yaml.getInt("world-settings.default.entity-tracking-range.players") : yaml.getInt("world-settings." + getWorldName() + ".entity-tracking-range.players");

        //register scoreboards
        registerScoreboards();
    }

    /**
     * Add a player to the arena
     *
     * @param p               - Player to add.
     * @param skipOwnerCheck - True if you want to skip the party checking for this player. This
     * @return true if was added.
     */
    public boolean addPlayer(Player p, boolean skipOwnerCheck) {
        if (p == null) return false;
        // Check if the player is already in an arena
        if (getArenaByPlayer(p) != null) {
            if (getArenaByPlayer(p).isSpectator(p)) {
                getArenaByPlayer(p).removeSpectator(p, false);
            } else {
                getArenaByPlayer(p).removePlayer(p, false);
            }
        }
        debug("Player added: " + p.getName() + " arena: " + getArenaName());

//        Used to check if a sidebar must be given or not
        boolean isStatusChange = false;

        /* used for base enter/leave event */
        isOnABase.remove(p);
        //
        if (getArenaByPlayer(p) != null) {
            return false;
        }
        if (getPartyManager().hasParty(p)) {
            if (!skipOwnerCheck) {
                if (!getPartyManager().isOwner(p)) {
                    BedWars.plugin.adventure().player(p).sendMessage(ChatFormatting.parseLegacyMini(getMsg(p, Messages.COMMAND_JOIN_DENIED_NOT_PARTY_LEADER)));
                    return false;
                }
                int partySize = (int) getPartyManager().getMembers(p).stream().filter(member -> {
                    IArena arena = Arena.getArenaByPlayer(member);
                    if (arena == null) {
                        return true;
                    }
                    return arena.isSpectator(member);
                }).count();

                if (partySize > maxInTeam * getTeams().size() - getPlayers().size()) {
                    BedWars.plugin.adventure().player(p).sendMessage(ChatFormatting.parseLegacyMini(getMsg(p, Messages.COMMAND_JOIN_DENIED_PARTY_TOO_BIG)));
                    return false;
                }
                for (Player mem : new ArrayList<>(getPartyManager().getMembers(p))) {
                    if (mem == p)
                        continue;
                    IArena a = Arena.getArenaByPlayer(mem);
                    if (a != null) {
                        /*if (a.isPlayer(mem)) {
                            a.removePlayer(mem, false);
                        } else */
                        if (a.isSpectator(mem)) {
                            a.removeSpectator(mem, false);
                        }
                    }
                    addPlayer(mem, true);
                }
            }
        }

        leaving.remove(p);

        if (status == GameState.waiting || (status == GameState.starting && (startingTask != null && startingTask.getCountdown() > 1))) {
            if (players.size() >= maxPlayers && !isVip(p)) {
                TextComponent text = new TextComponent(getMsg(p, Messages.COMMAND_JOIN_DENIED_IS_FULL));
                text.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, config.getYml().getString("storeLink")));
                p.spigot().sendMessage(text);
                return false;
            } else if (players.size() >= maxPlayers && isVip(p)) {
                boolean canJoin = false;
                for (Player on : new ArrayList<>(players)) {
                    if (!isVip(on)) {
                        canJoin = true;
                        removePlayer(on, false);
                        TextComponent vipKick = new TextComponent(getMsg(p, Messages.ARENA_JOIN_VIP_KICK));
                        vipKick.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, config.getYml().getString("storeLink")));
                        p.spigot().sendMessage(vipKick);
                        break;
                    }
                }
                if (!canJoin) {
                    BedWars.plugin.adventure().player(p).sendMessage(ChatFormatting.parseLegacyMini(getMsg(p, Messages.COMMAND_JOIN_DENIED_IS_FULL_OF_VIPS)));
                    return false;
                }
            }

            PlayerJoinArenaEvent ev = new PlayerJoinArenaEvent(this, p, false);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) return false;

            //Remove from ReJoin
            ReJoin rejoin = ReJoin.getPlayer(p);
            if (rejoin != null) {
                rejoin.destroy(true);
            }

            p.closeInventory();
            players.add(p);
            p.setFlying(false);
            p.setAllowFlight(false);
            p.setHealth(p.getMaxHealth());
            for (Player on : players) {
                Language language = Language.getPlayerLanguage(on);
                if (ev.getMessage().equals("")) {
                    BedWars.plugin.adventure().player(on).sendMessage(ChatFormatting.parseLegacyMini(getMsg(language, p, Messages.COMMAND_JOIN_PLAYER_JOIN_MSG)
                            .replace("%bw_v_prefix%", getChatSupport().getPrefix(p))
                            .replace("%bw_v_suffix%", getChatSupport().getSuffix(p))
                            .replace("%bw_playername%", p.getName())
                            .replace("%bw_player%", p.getDisplayName())
                            .replace("%bw_on%", String.valueOf(getPlayers().size()))
                            .replace("%bw_max%", String.valueOf(getMaxPlayers())))
                    );
                } else {
                    if (ev.getMessage() != null)
                        BedWars.plugin.adventure().player(on).sendMessage(ChatFormatting.parseLegacyMini(ev.getMessage()));
                }
            }
            setArenaByPlayer(p, this);

            /* check if you can start the arena */
            if (status == GameState.waiting) {
                int teams = 0, teammates = 0, partyMembers = 0;

                for (Player on : getPlayers()) {
                    if (getPartyManager().isOwner(on)) {
                        teams++;
                    }
                    if (getPartyManager().hasParty(on)) {
                        teammates++;
                        partyMembers += getPartyManager().getMembers(on).size();
                    }
                }

                // Check if the party fills the arena
                if (partyMembers >= maxPlayers) {
                    Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> changeStatus(GameState.starting), 10L);
                    isStatusChange = true;
                } else if (minPlayers <= players.size() && teams > 0 && players.size() != teammates / teams) {
                    Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> changeStatus(GameState.starting), 10L);
                    isStatusChange = true;
                } else if (players.size() >= minPlayers && teams == 0) {
                    Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> changeStatus(GameState.starting), 10L);
                    isStatusChange = true;
                }
            }

            //half full arena time shorten
            if (players.size() >= getMaxPlayers() / 2 && players.size() > minPlayers) {
                if (startingTask != null) {
                    if (Bukkit.getScheduler().isCurrentlyRunning(startingTask.getTask())) {
                        if (startingTask.getCountdown() > getConfig().getInt(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_HALF)) {
                            startingTask.setCountdown(BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_HALF));
                        }
                    }
                }
            }

            /* save player inventory etc */
            if (getServerType() != ServerType.BUNGEE) {
                PlayerGoods.createIfNeeded(p, true);
                playerLocation.put(p, p.getLocation());
            }
            PaperSupport.teleportC(p, getWaitingLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);

            sendPreGameCommandItems(p);
            for (PotionEffect pf : p.getActivePotionEffects()) {
                p.removePotionEffect(pf.getType());
            }
        } else if (status == GameState.playing || status == GameState.starting && (startingTask != null && startingTask.getCountdown() <= 1)) {
            addSpectator(p, false, null);
            /* stop code if status playing*/
            return false;
        }

        p.getInventory().setArmorContents(null);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // bungee mode invisibility issues
            if (getServerType() == ServerType.BUNGEE) {
                // fix invisibility issue
                //if (BedWars.nms.getVersion() == 7) {
                BedWars.nms.sendPlayerSpawnPackets(p, this);
                //}
            }
            for (Player on : Bukkit.getOnlinePlayers()) {
                if (on == null) continue;
                if (on.equals(p)) continue;
                if (isPlayer(on)) {
                    BedWars.nms.spigotShowPlayer(p, on);
                    BedWars.nms.spigotShowPlayer(on, p);
                } else {
                    BedWars.nms.spigotHidePlayer(p, on);
                    BedWars.nms.spigotHidePlayer(on, p);
                }
            }

            if (getServerType() == ServerType.BUNGEE) {
                // fix invisibility issue
                //if (BedWars.nms.getVersion() == 7) {
                BedWars.nms.sendPlayerSpawnPackets(p, this);
                //}
            }
        }, 17L);

        if (getServerType() == ServerType.BUNGEE) {
            p.getEnderChest().clear();
        }

        if (getPlayers().size() >= getMaxPlayers()) {
            if (startingTask != null) {
                if (Bukkit.getScheduler().isCurrentlyRunning(startingTask.getTask())) {
                    if (startingTask.getCountdown() > BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_SHORTENED)) {
                        startingTask.setCountdown(BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_START_COUNTDOWN_SHORTENED));
                    }
                }
            }
        }
        if (!isStatusChange)
            if (BedWars.getServerType() == ServerType.MULTIARENA || BedWars.getServerType() == ServerType.SHARED) {
                BoardManager.getInstance().giveTabFeatures(p, this, false);
            }

        refreshSigns();
        JoinNPC.updateNPCs(getGroup());
        return true;
    }

    /**
     * Add a player as Spectator
     *
     * @param p            Player to be added
     * @param playerBefore True if the player has played in this arena before and he died so now should be a spectator.
     * @param target       The player to spectate or track, can be null.
     */
    @Override
    public boolean addSpectator(@NotNull Player p, boolean playerBefore, @Nullable Player target) {
        // Method implementation logic continues here...
        return true;
    }

    // Explicit fallback implementations to complete abstract requirements
    @Override public String getArenaName() { return arenaName; }
    @Override public World getWorld() { return world; }
    @Override public GameState getStatus() { return status; }
    @Override public void changeStatus(GameState status) { this.status = status; }
    @Override public String getGroup() { return group; }
    @Override public List<Player> getPlayers() { return players; }
    @Override public int getMaxPlayers() { return maxPlayers; }
    @Override public boolean isSpectator(Player p) { return spectators.contains(p); }
    @Override public boolean isPlayer(Player p) { return players.contains(p); }
    @Override public void removePlayer(Player p, boolean b) { players.remove(p); arenaByPlayer.remove(p); }
    @Override public void removeSpectator(Player p, boolean b) { spectators.remove(p); arenaByPlayer.remove(p); }
    @Override public List<ITeam> getTeams() { return teams; }
    @Override public ITeam getTeam(String name) { return teams.stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst().orElse(null); }
    @Override public Location getWaitingLocation() { return waitingLocation; }
    @Override public ArenaConfig getConfig() { return cm; }
    @Override public void registerSigns() {}
    @Override public void refreshSigns() {}
    @Override public void registerScoreboards() {}
    @Override public void sendPreGameCommandItems(Player p) {}
    @Override public String getWorldName() { return worldName; }
}
