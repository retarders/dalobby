package top.retarders.dalobby;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class DaLobby extends JavaPlugin {
    enum Game {
        MIXED() {
            @Override
            public String toString() {
                return "Mixed";
            }
        },
        TNTTAG() {
            @Override
            public String toString() {
                return "TNT Tag";
            }
        };
    }

    class GameSign {
        public String server;
        public Game gamemode;
        public int gameId;
        public Sign block;

        public int players = 0;
        public int maxPlayers = 420;

        public GameSign(String server, Sign block) {
            this.server = server;
            this.block = block;

            // the server name convention is $gamemode-$id
            this.gamemode = Game.valueOf(server.split("-")[0].toUpperCase());
            this.gameId = Integer.parseInt(server.split("-")[1]);
        }

        public void send(Player player) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Sending you to " + this.server);

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(this.server);

            player.sendPluginMessage(
                DaLobby.getPlugin(DaLobby.class), "BungeeCord", out.toByteArray());
        }

        public void fetch() {
            // this is async
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PlayerCount");
            out.writeUTF(this.server);

            Bukkit.getServer().sendPluginMessage(
                DaLobby.getPlugin(DaLobby.class), "BungeeCord", out.toByteArray());
        }

        public void refresh() {
            this.block.setLine(
                0, ChatColor.DARK_PURPLE + ChatColor.BOLD.toString() + this.gamemode.toString());

            this.block.setLine(1,
                ChatColor.LIGHT_PURPLE.toString() + this.players + ChatColor.RESET + "/"
                    + ChatColor.DARK_PURPLE + this.maxPlayers);

            this.block.update();
        }
    }

    private List<GameSign> signs = new ArrayList<>();

    public void loadSign(int x, int y, int z) {
        Sign sign = (Sign) this.getServer().getWorld("world").getBlockAt(x, y, z).getState();
        String server = sign.getLine(0).replace("[", "").replace("]", "");

        this.signs.add(new GameSign(server, sign));
    }

    public void loadSigns() {
        this.loadSign(1, 61, 5);
        this.loadSign(-1, 61, 5);
    }

    @Override
    public void onEnable() {
        this.loadSigns();

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(
            this, "BungeeCord", (channel, player, message) -> {
                if (channel != "BungeeCord")
                    return;

                ByteArrayDataInput in = ByteStreams.newDataInput(message);
                String subchannel = in.readUTF();

                if (subchannel == "PlayerCount") {
                    String server = in.readUTF();
                    int playercount = in.readInt();

                    Optional<GameSign> signOpt =
                        signs.stream().filter(sign -> sign.server == server).findFirst();

                    if (signOpt.isPresent()) {
                        GameSign sign = signOpt.get();

                        sign.players = playercount;

                        sign.refresh();
                    }
                }
            });

        this.getServer().getScheduler().scheduleSyncRepeatingTask(
            this, () -> signs.forEach(GameSign::fetch), 20L * 3, 20L * 3);

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
            public void onInteract(PlayerInteractEvent event) {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                    && event.getClickedBlock().getType() == Material.WALL_SIGN) {
                    Optional<GameSign> firstOpt =
                        signs.stream()
                            .filter(sign
                                -> sign.block.getLocation().equals(
                                    event.getClickedBlock().getLocation()))
                            .findFirst();

                    if (firstOpt.isPresent()) {
                        GameSign gameSign = firstOpt.get();

                        gameSign.send(event.getPlayer());
                    }
                }
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

            @EventHandler
            public void onFoodLevelChange(FoodLevelChangeEvent event) {
                event.setCancelled(true);
            }
        }, this);
    }
}
