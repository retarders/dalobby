package top.retarders.dalobby;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DaLobby extends JavaPlugin {
    @Override
    public void onEnable() {
        Location spawnpoint =
            this.getServer().getWorld("world").getHighestBlockAt(0, 0).getLocation().add(0, 3, 0);

        this.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                Player player = event.getPlayer();

                event.getPlayer().teleport(spawnpoint);
                event.getPlayer().playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 1);

                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&7Welcome to the &d&lRetarders Network&7, &7the &fofficial &7retarders server"));
                player.sendMessage(ChatColor.translateAlternateColorCodes(
                    '&', "&7We hope you enjoy your stay, press any signs to join games"));
            }

            @EventHandler
            public void onDamage(EntityDamageEvent event) {
                if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                    event.setDamage(0);
                    return;
                }

                if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                    event.getEntity().teleport(spawnpoint);
                }

                event.setCancelled(true);
            }

            @EventHandler
            public void onBlockBreak(BlockBreakEvent event) {
                event.setCancelled(true);
            }

            @EventHandler
            public void onBlockPlace(BlockPlaceEvent event) {
                event.setCancelled(true);
            }
        }, this);
    }
}
