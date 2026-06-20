package com.tomkeuper.bedwars.shop.listeners;

import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.shop.quickbuy.PlayerQuickBuyCache;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class ShopOpenListener implements Listener {

    @EventHandler
    public void onShopOpen(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        IArena a = Arena.getArenaByPlayer(player);[cite: 2]

        if (a == null) return;[cite: 2]
        if (!a.getStatus().equals(GameState.playing)) return;[cite: 2]
        if (!a.isPlayer(player)) return;[cite: 2]

        Location clickLoc = e.getRightClicked().getLocation();[cite: 2]

        // 1. Check Team Shops
        for (ITeam t : a.getTeams()) {[cite: 2]
            for (Location shopLoc : t.getShops()) {[cite: 2]
                if (isSameBlock(clickLoc, shopLoc)) {[cite: 2]
                    e.setCancelled(true);[cite: 2]
                    openShop(a, player);[cite: 2]
                    return;
                }
            }
        }

        // 2. Check Public Shops
        for (Location shopLoc : a.getPublicShops()) {
            if (isSameBlock(clickLoc, shopLoc)) {
                e.setCancelled(true);
                openShop(a, player);
                return;
            }
        }

        // 3. Check Public Upgrades (If your upgrade logic is handled here)
        for (Location upgradeLoc : a.getPublicUpgrades()) {
            if (isSameBlock(clickLoc, upgradeLoc)) {
                e.setCancelled(true);
                if (a.getLinkedUpgrades() != null) {
                    a.getLinkedUpgrades().open(player);
                }
                return;
            }
        }
    }

    private boolean isSameBlock(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();[cite: 2]
    }

    private void openShop(IArena a, Player player) {
        if (a.getLinkedShop() != null) {[cite: 2]
            a.getLinkedShop().open(
                    player,
                    PlayerQuickBuyCache.getInstance().getQuickBuyCache(player.getUniqueId()),[cite: 2]
                    true[cite: 2]
            );
        }
    }
}