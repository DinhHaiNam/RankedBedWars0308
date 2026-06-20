package com.tomkeuper.bedwars.upgrades.listeners;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.arena.Arena;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class UpgradeOpenListener implements Listener {

    @EventHandler
    public void onUpgradesOpen(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        IArena a = Arena.getArenaByPlayer(player);

        if (a == null) return;
        if (!a.getStatus().equals(GameState.playing)) return;

        Location l = e.getRightClicked().getLocation();

        // 1. Check Team Upgrades
        for (ITeam t : a.getTeams()) {
            for (Location l2 : t.getUpgrades()) {
                if (l2 == null || l2.getWorld() == null) continue;

                if (isSameBlock(l, l2)) {
                    e.setCancelled(true);
                    if (a.isPlayer(player)) {
                        openUpgradesMenu(a, player);
                    }
                    return;
                }
            }
        }

        // 2. Check Public Upgrades
        if (a instanceof Arena) {
            Arena arenaImpl = (Arena) a;
            for (Location l2 : arenaImpl.getPublicUpgrades()) {
                if (l2 == null || l2.getWorld() == null) continue;

                if (isSameBlock(l, l2)) {
                    e.setCancelled(true);
                    if (a.isPlayer(player)) {
                        openUpgradesMenu(a, player);
                    }
                    return;
                }
            }
        }
    }

    private boolean isSameBlock(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }

    private void openUpgradesMenu(IArena arena, Player player) {
        BedWars.getUpgradeManager()
                .getMenuForArena(arena)
                .open(player);
    }
}