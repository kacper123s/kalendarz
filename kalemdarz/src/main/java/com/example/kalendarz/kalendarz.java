package com.example.adventcalendar;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class AdventCalendarPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, String> playerCalendars = new HashMap<>(); // Typ kalendarza przypisany do gracza
    private final Map<UUID, Set<Integer>> openedDays = new HashMap<>(); // Otworzone dni dla graczy
    private final Map<Integer, ItemStack> freeRewards = new HashMap<>(); // Nagrody darmowe
    private final Map<Integer, ItemStack> premiumRewards = new HashMap<>(); // Nagrody premium

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        setupRewards();
        getLogger().info("AdventCalendarPlugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AdventCalendarPlugin disabled!");
    }

    private void setupRewards() {
        // Konfiguracja nagród dla kalendarza darmowego
        for (int i = 1; i <= 24; i++) {
            ItemStack reward = new ItemStack(Material.COAL, i); // Przykład: Węgiel
            ItemMeta meta = reward.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Darmowa nagroda dnia " + i);
                reward.setItemMeta(meta);
            }
            freeRewards.put(i, reward);
        }

        // Konfiguracja nagród dla kalendarza premium
        for (int i = 1; i <= 24; i++) {
            ItemStack reward = new ItemStack(Material.DIAMOND, i); // Przykład: Diamenty
            ItemMeta meta = reward.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("Premium nagroda dnia " + i);
                reward.setItemMeta(meta);
            }
            premiumRewards.put(i, reward);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("kalendarz")) {
            if (args.length == 2) {
                String playerName = args[0];
                String calendarType = args[1].toLowerCase();

                Player targetPlayer = Bukkit.getPlayer(playerName);
                if (targetPlayer == null) {
                    sender.sendMessage("Gracz " + playerName + " nie jest online.");
                    return true;
                }

                if (!calendarType.equals("darmowy") && !calendarType.equals("premium")) {
                    sender.sendMessage("Typ kalendarza musi być 'darmowy' lub 'premium'.");
                    return true;
                }

                playerCalendars.put(targetPlayer.getUniqueId(), calendarType);
                sender.sendMessage("Przypisano kalendarz " + calendarType + " dla gracza " + playerName + ".");
                targetPlayer.sendMessage("Otrzymałeś kalendarz typu: " + calendarType + "!");
                return true;
            } else if (args.length == 0 && sender instanceof Player) {
                Player player = (Player) sender;
                String calendarType = playerCalendars.get(player.getUniqueId());

                if (calendarType == null) {
                    player.sendMessage("Nie masz przypisanego kalendarza.");
                    return true;
                }

                openCalendar(player, calendarType);
                return true;
            } else {
                sender.sendMessage("Użycie: /kalendarz <gracz> <darmowy|premium>");
                return true;
            }
        }
        return false;
    }

    private void openCalendar(Player player, String calendarType) {
        Inventory calendar = Bukkit.createInventory(null, 27, "Kalendarz Adwentowy: " + calendarType);

        Map<Integer, ItemStack> rewards = calendarType.equals("premium") ? premiumRewards : freeRewards;

        for (int i = 1; i <= 24; i++) {
            ItemStack item;
            if (openedDays.containsKey(player.getUniqueId()) && openedDays.get(player.getUniqueId()).contains(i)) {
                item = new ItemStack(Material.BARRIER);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("Już otworzyłeś dzień " + i);
                    item.setItemMeta(meta);
                }
            } else {
                item = new ItemStack(Material.CHEST);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("Dzień " + i);
                    item.setItemMeta(meta);
                }
            }
            calendar.setItem(i - 1, item);
        }

        player.openInventory(calendar);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("Kalendarz Adwentowy")) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        String calendarType = playerCalendars.get(player.getUniqueId());
        if (calendarType == null) return;

        int slot = event.getRawSlot() + 1;

        if (slot < 1 || slot > 24) return;

        if (openedDays.containsKey(player.getUniqueId()) && openedDays.get(player.getUniqueId()).contains(slot)) {
            player.sendMessage("Już odebrałeś nagrodę z dnia " + slot + "!");
            return;
        }

        Map<Integer, ItemStack> rewards = calendarType.equals("premium") ? premiumRewards : freeRewards;

        if (rewards.containsKey(slot)) {
            player.getInventory().addItem(rewards.get(slot));
            openedDays.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(slot);
            player.sendMessage("Odebrałeś nagrodę z dnia " + slot + "!");
        } else {
            player.sendMessage("Nie ma nagrody dla tego dnia.");
        }
    }
}
