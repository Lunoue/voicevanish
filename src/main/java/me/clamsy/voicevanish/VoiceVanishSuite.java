
package me.clamsy.voicevanish;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class VoiceVanishSuite extends JavaPlugin implements Listener {

    private FileConfiguration cfg;
    private ProtocolManager protocolManager;
    private boolean protocolEnabled;
    private List<String> keywords;
    private boolean filterCommands;
    private Set<String> intercept;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cfg = getConfig();
        filterCommands = cfg.getBoolean("filterCommands", true);
        intercept = cfg.getStringList("commandsToIntercept").stream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        protocolEnabled = cfg.getBoolean("protocolLib.enabled", false);
        keywords = cfg.getStringList("protocolLib.keywords");
        if (protocolEnabled && Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            tryEnableProtocolFilter();
        } else {
            getLogger().info("[VVS] ProtocolLib suppression disabled.");
        }

        if (filterCommands) {
            Bukkit.getPluginManager().registerEvents(this, this);
        }
        getLogger().info("[VVS] Enabled");
    }

    @Override
    public void onDisable() {
        if (protocolManager != null) {
            try {
                protocolManager.removePacketListeners(this);
            } catch (Throwable ignored) {}
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCmd(PlayerCommandPreprocessEvent e) {
        if (!filterCommands) return;
        String raw = e.getMessage().trim();
        String low = raw.toLowerCase(Locale.ROOT);
        if (!intercept.contains(low)) return;

        e.setCancelled(true);
        Player requester = e.getPlayer();

        List<String> visible = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !isVanishedEssentials(p))
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        requester.sendMessage(ChatColor.AQUA + "Игроки (ваниш скрыт):");
        requester.sendMessage(ChatColor.WHITE + (visible.isEmpty()
                ? ChatColor.GRAY + "Нет доступных игроков"
                : String.join(ChatColor.GRAY + ", " + ChatColor.WHITE, visible)));
        requester.sendMessage(ChatColor.DARK_GRAY + "TAB — автодополнение.");
    }

    private boolean isVanishedEssentials(Player p) {
        try {
            Plugin essPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (essPlugin == null || !essPlugin.isEnabled()) return false;

            Class<?> essCls = Class.forName("com.earth2me.essentials.Essentials");
            if (!essCls.isInstance(essPlugin)) return false;

            Method getUser = essCls.getMethod("getUser", Player.class);
            Object user = getUser.invoke(essPlugin, p);
            if (user == null) return false;

            Method isVanished = user.getClass().getMethod("isVanished");
            Object res = isVanished.invoke(user);
            return res instanceof Boolean && (Boolean) res;
        } catch (Throwable ignored) {}
        return false;
    }

    private void tryEnableProtocolFilter() {
        try {
            protocolManager = ProtocolLibrary.getProtocolManager();
            protocolManager.addPacketListener(new PacketAdapter(this,
                    PacketType.Play.Server.SYSTEM_CHAT, PacketType.Play.Server.CHAT) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    if (keywords == null || keywords.isEmpty()) return;
                    PacketContainer packet = event.getPacket();
                    String plain = extractPlain(packet);
                    if (plain == null) return;

                    String lower = plain.toLowerCase(Locale.ROOT);
                    boolean hit = keywords.stream().map(String::toLowerCase).anyMatch(lower::contains);
                    if (!hit) return;

                    Player culprit = guessPlayerFromText(plain);
                    if (culprit != null && isVanishedEssentials(culprit)) {
                        event.setCancelled(true);
                    }
                }
            });
            getLogger().info("[VVS] ProtocolLib suppression enabled.");
        } catch (Throwable t) {
            getLogger().warning("[VVS] ProtocolLib suppression failed: " + t.getMessage());
        }
    }

    private String extractPlain(PacketContainer packet) {
        try {
            if (packet.getChatComponents().size() > 0 && packet.getChatComponents().read(0) != null) {
                return packet.getChatComponents().read(0).getJson();
            }
            if (packet.getStrings().size() > 0 && packet.getStrings().read(0) != null) {
                return packet.getStrings().read(0);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private Player guessPlayerFromText(String plain) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plain.contains(p.getName())) return p;
        }
        return null;
    }
}
