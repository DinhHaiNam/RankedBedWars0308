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
        IArena a = Arena.getArenaByPlayer(player); 

        if (a == null) return; 
        if (!a.getStatus().equals(GameState.playing)) return; 
        if (!a.isPlayer(player)) return; 

        Location clickLoc = e.getRightClicked().getLocation(); 

        // 1. Check Team Shops
        for (ITeam t : a.getTeams()) { 
            for (Location shopLoc : t.getShops()) { 
                if (isSameBlock(clickLoc, shopLoc)) { 
                    e.setCancelled(true); 
                    openShop(a, player); 
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
                && loc1.getBlockZ() == loc2.getBlockZ(); 
    }

    private void openShop(IArena a, Player player) {
        if (a.getLinkedShop() != null) { 
            a.getLinkedShop().open(
                    player,
                    PlayerQuickBuyCache.getInstance().getQuickBuyCache(player.getUniqueId()), 
                    true 
            );
        }
    }
}
